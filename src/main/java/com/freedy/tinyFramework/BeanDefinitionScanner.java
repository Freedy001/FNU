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

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/2 15:52
 */
public class BeanDefinitionScanner implements Scanner {

    private AbstractApplication abstractApplication;


    public BeanDefinitionScanner(AbstractApplication abstractApplication) {
        this.abstractApplication = abstractApplication;
    }

    @Override
    public void scan(String... packagesName) {
        scan(packagesName, null);
    }

    @Override
    public void scan(String[] PackageName, String[] exclude) {

        List<Class<?>> classList = doScan(PackageName, exclude);
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
                BeanDefinition definition = new PropertiesBeanDefinition(beanName, beanClass, properties.value());
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
            if (method.getAnnotation(PostConstruct.class) != null) {
                if (method.getParameterCount() != 0)
                    throw new BeanException("post-construct method should not have parameters");
                beanDefinition.setPostConstruct(method);
            }
            if (method.getAnnotation(Inject.class) != null)
                beanDefinition.addInjectMethods(method);
        }
    }


    private String convertName(String className) {
        return className.substring(0, 1).toLowerCase(Locale.ROOT) + className.substring(1);
    }


    private List<Class<?>> doScan(@NonNull String[] PackageNames, String[] exclude) {

        ClassLoader classLoader = BeanDefinitionScanner.class.getClassLoader();
        List<Class<?>> list = new ArrayList<>();

        for (String PackageName : PackageNames) {
            String[] packSplit = PackageName.split("\\.");
            String lastPackageName = packSplit[packSplit.length - 1];
            try {
                Files.walk(Paths.get(Objects.requireNonNull(classLoader.getResource(PackageName.replaceAll("\\.", "/"))).toURI())).forEach(pa -> {
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

                        try {
                            String fullClassName = PackageName + "." + joiner;
                            if (exclude != null) {
                                for (String s : exclude) {
                                    if (fullClassName.contains(s))return;
                                }
                            }

                            Class<?> aClass = Class.forName(fullClassName);
                            list.add(aClass);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    @Override
    public AbstractApplication getApplication() {
        return abstractApplication;
    }

    @Override
    public void setApplication(AbstractApplication application) {
        abstractApplication=application;
    }

}
