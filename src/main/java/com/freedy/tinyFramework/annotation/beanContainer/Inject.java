package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/2 15:41
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    //通过名称注入 不填按照类型注入
    String value() default "";
    //仅当标注在方法上有用，当发生异常整个程序将会终止
    boolean failFast() default false;
}
