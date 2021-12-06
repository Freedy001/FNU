package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/4 22:28
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {
}
