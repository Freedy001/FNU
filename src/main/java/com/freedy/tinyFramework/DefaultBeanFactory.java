package com.freedy.tinyFramework;

import com.freedy.tinyFramework.beanFactory.BeanFactory;
import com.freedy.tinyFramework.exception.NoSuchBeanException;
import com.freedy.tinyFramework.exception.NoUniqueBeanException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认bean工厂 持有bean的容器
 *
 * @author Freedy
 * @date 2021/12/4 10:04
 */
public abstract class DefaultBeanFactory implements BeanFactory {

    private final Map<String, Object> singletonObject = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> typeSingletonObject = new ConcurrentHashMap<>();


    protected void registerBean(String beanName, Object bean) {
        Object containerBean;
        if ((containerBean = singletonObject.put(beanName, bean)) != null)
            throw new NoUniqueBeanException("same bean name!your bean " + bean + " container bean " + containerBean);
        Set<Class<?>> superClass = ReflectionUtils.getClassRecursion(bean);
        for (Class<?> sClass : superClass) {
            if (!sClass.getName().equals("java.lang.Object")) {
                //添加所有父类到容器中
                typeSingletonObject.merge(sClass, beanName, (o, n) -> o + "," + n);
            }
        }
    }

    @Override
    public Object getBean(String beanName){
        return singletonObject.get(beanName);
    }

    @Override
    public <T> T getBean(String beanName, Class<T> beanType) {
        return beanType.cast(getBean(beanName));
    }

    @Override
    public <T> T getBean(Class<T> beanType) {
        String[] beanNames = Optional.ofNullable(typeSingletonObject.get(beanType)).orElse("").split(",");
        if (beanNames.length == 0)
            throw new NoSuchBeanException("there are no bean for type ?", beanType.getSimpleName());
        if (beanNames.length > 1)
            throw new NoUniqueBeanException("find ? beans for type ? in container,please specify one!", beanNames.length, beanType.getSimpleName());
        return beanType.cast(getBean(beanNames[0]));
    }

    @Override
    public <T> List<T> getBeansForType(Class<T> beanType) {
        String[] beanNames = Optional.ofNullable(typeSingletonObject.get(beanType)).orElse("").split(",");
        ArrayList<T> ts = new ArrayList<>();
        for (String beanName : beanNames) {
            if (StringUtils.hasText(beanName)) {
                ts.add(beanType.cast(getBean(beanName)));
            }
        }
        return ts;
    }


    @Override
    public Set<String> getAllBeanNames() {
        return singletonObject.keySet();
    }

    @Override
    public boolean containsBean(String beanName) {
        return singletonObject.containsKey(beanName);
    }

    @Override
    public boolean containsBean(Class<?> beanType) {
        return typeSingletonObject.containsKey(beanType);
    }
}
