package com.freedy.tinyFramework.annotation.interceptor;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/7 14:46
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    /**
     * bean名称
     */
    String value() default "";
}
