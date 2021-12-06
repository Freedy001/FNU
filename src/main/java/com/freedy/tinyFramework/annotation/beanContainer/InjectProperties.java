package com.freedy.tinyFramework.annotation.beanContainer;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/5 16:01
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Part(type = BeanType.SINGLETON,configure = false)
public @interface InjectProperties {
    //属性前缀
    String value();

    String beanName() default "";
}
