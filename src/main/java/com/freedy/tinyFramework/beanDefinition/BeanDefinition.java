package com.freedy.tinyFramework.beanDefinition;

import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/4 14:42
 */
@Data
public abstract class BeanDefinition {

    protected String beanName;
    protected Class<?> beanClass;
    protected BeanType type=BeanType.SINGLETON;
    protected Method postConstruct;
    protected List<Method> injectMethods;

    public BeanDefinition(){}

    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    public void addInjectMethods(Method injectMethod){
        if (injectMethods==null){
            injectMethods=new ArrayList<>();
        }
        injectMethods.add(injectMethod);
    }



}
