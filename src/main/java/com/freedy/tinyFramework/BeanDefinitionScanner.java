package com.freedy.tinyFramework;

import com.freedy.tinyFramework.annotation.beanContainer.*;
import com.freedy.tinyFramework.annotation.mvc.REST;
import com.freedy.tinyFramework.beanDefinition.BeanDefinition;
import com.freedy.tinyFramework.beanDefinition.ConfigBeanDefinition;
import com.freedy.tinyFramework.beanDefinition.NormalBeanDefinition;
import com.freedy.tinyFramework.beanDefinition.PropertiesBeanDefinition;
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

    AbstractApplication abstractApplication;
    Class<?> baseClass;

    public BeanDefinitionScanner(Class<?> baseClass, AbstractApplication abstractApplication) {
        this.abstractApplication = abstractApplication;
        this.baseClass = baseClass;
        String fullClassName = baseClass.getName();
        scan(fullClassName.substring(0, fullClassName.lastIndexOf(".")));
    }

    @Override
    public void scan(String... packagesName) {
        for (String name : packagesName) {
            List<Class<?>> classList = doScan(name);
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
                    if (part.configure()){
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
                if (properties==null) continue;
                String beanName = properties.value();
                if (StringUtils.isEmpty(beanName)) {
                    beanName = convertName(beanClass.getSimpleName());
                }
                BeanDefinition definition = new PropertiesBeanDefinition(beanName, beanClass,properties.value());
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


    private List<Class<?>> doScan(@NonNull String PackageName, String... exclude) {
        ClassLoader classLoader = baseClass.getClassLoader();
        List<Class<?>> list = new ArrayList<>();
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
                        Class<?> aClass = Class.forName(PackageName + "." + joiner);
                        list.add(aClass);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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
