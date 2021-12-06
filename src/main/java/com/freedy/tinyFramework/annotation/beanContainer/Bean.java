package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/2 21:03
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    //bean name
    String value() default "";

    BeanType type() default BeanType.SINGLETON;

    Class<?> conditionalOnMissBeanByTyp() default Bean.class;
    String conditionalOnMissBeanByName() default "";
    Class<?> conditionalOnBeanByType() default Bean.class;
    String conditionalOnBeanByName() default "";
}
