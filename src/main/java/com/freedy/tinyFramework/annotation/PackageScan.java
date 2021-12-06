package com.freedy.tinyFramework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Freedy
 * @date 2021/12/2 15:49
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PackageScan {
    //需要被扫描的包名称
    String value();

    //需要被排除的包名
    String exclude() default "";
}
