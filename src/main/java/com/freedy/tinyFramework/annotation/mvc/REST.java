package com.freedy.tinyFramework.annotation.mvc;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/11/29 10:05
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface REST {
    //根rui 默认为空
    String value() default "";

    String beanName() default "";


}
