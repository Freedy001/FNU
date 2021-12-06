package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/2 15:34
 */
@Documented
@Target({ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Part {
    //bean名称
    String value() default "";

    BeanType type() default BeanType.SINGLETON;

    boolean configure() default false;

}
