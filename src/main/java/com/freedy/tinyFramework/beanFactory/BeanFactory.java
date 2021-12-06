package com.freedy.tinyFramework.beanFactory;

import java.util.List;
import java.util.Set;

/**
 * @author Freedy
 * @date 2021/12/4 9:57
 */
public interface BeanFactory {

    Object getBean(String beanName);

    <T> T getBean(String beanName,Class<T> beanType);

    <T> T getBean(Class<T> beanType);

    <T> List<T> getBeansForType(Class<T> beanType);

    boolean containsBean(String beanName);

    boolean containsBean(Class<?> beanType);

    boolean isSingleton(String name);

    boolean isPrototype(String name);

    Class<?> getType(String name);

    Set<String> getAllBeanNames();
}
