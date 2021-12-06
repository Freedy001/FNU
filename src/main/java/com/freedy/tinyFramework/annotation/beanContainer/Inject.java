package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/2 15:41
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    //仅当标注在Field上才有效
    String byName() default "";
}
