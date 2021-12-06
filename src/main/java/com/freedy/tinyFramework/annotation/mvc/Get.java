package com.freedy.tinyFramework.annotation.mvc;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/11/29 10:07
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Get {
    //请求uri，不填则为方法名
    String value() default "";

}
