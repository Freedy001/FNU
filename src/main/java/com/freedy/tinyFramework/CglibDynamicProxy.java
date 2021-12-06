package com.freedy.tinyFramework;

import com.freedy.tinyFramework.beanFactory.AbstractApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/12/3 16:33
 */
@Slf4j
public record CglibDynamicProxy(Object bean, AbstractApplication abstractApplication) {



    public Object newNettyHandleProxy(Class<?> handleType) {

        return bean;
    }


}
