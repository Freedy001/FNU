package com.freedy.tinyFramework.annotation.interceptor;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/2 15:23
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pre {
    String interceptEL();
}
