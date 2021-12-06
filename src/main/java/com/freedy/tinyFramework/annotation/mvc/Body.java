package com.freedy.tinyFramework.annotation.mvc;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/11/29 11:24
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Body {
}
