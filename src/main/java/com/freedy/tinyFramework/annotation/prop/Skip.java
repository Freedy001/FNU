package com.freedy.tinyFramework.annotation.prop;

import java.lang.annotation.*;

/**
 * 跳过属性字段的解析
 * @author Freedy
 * @date 2021/12/9 9:49
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Skip {
}
