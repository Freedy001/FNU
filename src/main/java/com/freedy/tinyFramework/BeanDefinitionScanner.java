package com.freedy.tinyFramework;

import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
import com.freedy.tinyFramework.annotation.interceptor.After;
import com.freedy.tinyFramework.annotation.interceptor.Around;
import com.freedy.tinyFramework.annotation.interceptor.Aspect;
import com.freedy.tinyFramework.annotation.interceptor.Pre;
import com.freedy.tinyFramework.annotation.mvc.REST;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.beanDefinition.*;
import com.freedy.tinyFramework.beanFactory.AbstractApplication;
import com.freedy.tinyFramework.exception.BeanException;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.NonNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2021/12/2 15:52
 */
@SuppressWarnings({"CallToPrintStackTrace", "resource"})
public class BeanDefinitionScanner implements Scanner {

    private static final List<String> classNames = new ArrayList<>();

    static {
        if (Optional.ofNullable(System.getProperty("org.graalvm.nativeimage.imagecode")).orElse("").equals("buildtime")) {
            for (String name : scanClassName(new String[]{"com.freedy"}, new String[]{"com.freedy.tinyFramework"})) {
                System.out.println("[BUILD-TIME] add to " + name + " package search path");
                classNames.add(name);
            }
        }
    }


    private AbstractApplication abstractApplication;


    public BeanDefinitionScanner(AbstractApplication abstractApplication) {
        this.abstractApplication = abstractApplication;
    }

    @Override
    public void scan(String... packagesName) {
        scan(packagesName, null);
    }

    @Override
    public void scan(String[] packagesName, String[] exclude) {

        List<Class<?>> classList = doScan(packagesName, exclude);
        for (Class<?> beanClass : classList) {
            Part part = beanClass.getAnnotation(Part.class);
            if (part != null) {
                String beanName = part.value();
                if (StringUtils.isEmpty(beanName)) {
                    beanName = convertName(beanClass.getSimpleName());
                }
                NormalBeanDefinition definition = new NormalBeanDefinition(beanName, beanClass);
                definition.setType(part.type());
                definition.setIsConfigure(part.configure());
                findInjectMethodOrPostConstructMethod(beanClass, definition);
                abstractApplication.registerBeanDefinition(definition);
                //配置类注入
                if (part.configure()) {
                    for (Method method : beanClass.getDeclaredMethods()) {
                        Bean bean = method.getAnnotation(Bean.class);
                        if (bean == null) continue;
                        abstractApplication.registerBeanDefinition(new ConfigBeanDefinition(method, bean));
                    }
                }
                continue;
            }
            REST rest = beanClass.getAnnotation(REST.class);
            if (rest != null) {
                String beanName = rest.beanName();
                if (StringUtils.isEmpty(beanName)) {
                    beanName = convertName(beanClass.getSimpleName());
                }
                NormalBeanDefinition definition = new NormalBeanDefinition(beanName, beanClass);
                definition.setIsRest(true);
                findInjectMethodOrPostConstructMethod(beanClass, definition);
                abstractApplication.registerBeanDefinition(definition);
                continue;
            }
            InjectProperties properties = beanClass.getAnnotation(InjectProperties.class);
            if (properties != null) {
                String beanName = properties.beanName();
                if (StringUtils.isEmpty(beanName)) {
                    beanName = convertName(beanClass.getSimpleName());
                }
                BeanDefinition definition = new PropertiesBeanDefinition(
                        beanName,
                        beanClass,
                        properties.value(),
                        properties.nonePutIfEmpty(),
                        properties.exclude()
                );
                findInjectMethodOrPostConstructMethod(beanClass, definition);
                abstractApplication.registerBeanDefinition(definition);
                continue;
            }
            Aspect aspect = beanClass.getAnnotation(Aspect.class);
            if (aspect != null) {
                String beanName = aspect.value();
                if (StringUtils.isEmpty(beanName)) {
                    beanName = convertName(beanClass.getSimpleName());
                }
                ProxyBeanDefinition definition = new ProxyBeanDefinition(beanName, beanClass);
                findInjectMethodOrPostConstructMethod(beanClass, definition);

                for (Method method : beanClass.getDeclaredMethods()) {
                    Pre pre = method.getAnnotation(Pre.class);
                    After after = method.getAnnotation(After.class);
                    Around around = method.getAnnotation(Around.class);
                    if (pre != null) {
                        definition.add(new ProxyBeanDefinition.MataInterceptor(method, pre.interceptEL(), Pre.class.getName()));
                    }
                    if (after != null) {
                        definition.add(new ProxyBeanDefinition.MataInterceptor(method, after.interceptEL(), After.class.getName()));
                    }
                    if (around != null) {
                        definition.add(new ProxyBeanDefinition.MataInterceptor(method, around.interceptEL(), Around.class.getName()));
                    }
                }
                abstractApplication.registerBeanDefinition(definition);
            }
        }

    }

    private void findInjectMethodOrPostConstructMethod(Class<?> baseClass, BeanDefinition beanDefinition) {
        for (Method method : baseClass.getDeclaredMethods()) {
            PostConstruct post = method.getAnnotation(PostConstruct.class);
            if (post != null) {
                if (method.getParameterCount() != 0)
                    throw new BeanException("post-construct method should not have parameters");
                beanDefinition.addPostConstruct(new BeanDefinition.DelayMethod(method, post.failFast()));
            }
            Inject inject = method.getAnnotation(Inject.class);
            if (inject != null)
                beanDefinition.addInjectMethods(new BeanDefinition.DelayMethod(method, inject.failFast()));
        }
    }


    private String convertName(String className) {
        return className.substring(0, 1).toLowerCase(Locale.ROOT) + className.substring(1);
    }

    public static List<Class<?>> doScan(@NonNull String[] packageNames, String[] exclude) {
        return classNames.isEmpty() ? scanClassName(packageNames, exclude).stream().map(i -> {
            try {
                return Class.forName(i);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()) : nativeScan(packageNames, exclude);
    }

    private static List<String> scanClassName(String[] packageNames, String[] exclude) {
        ClassLoader classLoader = BeanDefinitionScanner.class.getClassLoader();
        String urls = Objects.requireNonNull(classLoader.getResource(BeanDefinitionScanner.class.getName().replace(".", "/") + ".class")).toString();
        String baseUrl = null;
        if (urls.contains("!/")) {
            baseUrl = urls.split("!/")[0] + "!/";
        } else if (urls.contains("classes/")) {
            baseUrl = urls.split("classes/")[0] + "/classes/";
        }

        List<String> list = new ArrayList<>();

        for (String PackageName : packageNames) {
            try {
                URL url = baseUrl == null ? classLoader.getResource(PackageName.replaceAll("\\.", "/")) : new URL(baseUrl + PackageName.replaceAll("\\.", "/"));
                assert url != null;
                String protocol = url.getProtocol();
                if (protocol.equals("file")) {
                    fileScan(exclude, list, PackageName, url);
                } else if (protocol.equals("jar")) {
                    jarScan(exclude, list, PackageName, url);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    /**
     * 普通环境扫描
     */
    private static void fileScan(String[] exclude, List<String> list, String PackageName, URL url) throws IOException, URISyntaxException {
        String[] packSplit = PackageName.split("\\.");
        String lastPackageName = packSplit[packSplit.length - 1];
        Files.walk(Paths.get(url.toURI())).forEach(pa -> {
            if (Files.isRegularFile(pa)) {
                String[] split = pa.toString().split("\\\\");
                int length = split.length;
                int index = length - 1;
                for (; index >= 0; index--) {
                    if (split[index].toLowerCase(Locale.ROOT).equals(lastPackageName.toLowerCase(Locale.ROOT))) {
                        break;
                    }
                }

                if (index == -1) return;
                StringJoiner joiner = new StringJoiner(".");
                for (int i = index + 1; i < length; i++) {
                    if (i == length - 1) {
                        String[] s = split[i].split("\\.");
                        if (!s[1].equals("class")) return;
                        joiner.add(s[0]);
                        break;
                    }
                    joiner.add(split[i]);
                }

                String fullClassName = PackageName + "." + joiner;
                if (exclude != null) {
                    for (String s : exclude) {
                        if (fullClassName.contains(s)) return;
                    }
                }

                list.add(fullClassName);
            }
        });
    }

    /**
     * jar环境扫描
     */
    private static void jarScan(String[] exclude, List<String> list, String PackageName, URL url) throws Exception {
        String pathName = PackageName.replace(".", "/");

        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarName = jarEntry.getName();
            if (jarName.contains(pathName) &&
                    !jarName.equals(pathName + "/") &&
                    !jarEntry.isDirectory() &&
                    jarName.endsWith(".class")) {
                String fullClazzName = jarName.replace("/", ".").replace(".class", "");
                if (exclude != null) {
                    for (String s : exclude) {
                        if (fullClazzName.contains(s)) return;
                    }
                }
                list.add(fullClazzName);
            }
        }
    }

    private static List<Class<?>> nativeScan(String[] include, String[] exclude) {
        return classNames.stream().filter(i -> {
            if (include == null) return false;
            for (String s : include) {
                if (i.startsWith(s)) return true;
            }
            return false;
        }).filter(i -> {
            if (exclude == null) return true;
            for (String s : exclude) {
                if (i.startsWith(s)) return false;
            }
            return true;
        }).map(i -> {
            try {
                return Class.forName(i);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }


    @Override
    public AbstractApplication getApplication() {
        return abstractApplication;
    }

    @Override
    public void setApplication(AbstractApplication application) {
        abstractApplication = application;
    }

}
