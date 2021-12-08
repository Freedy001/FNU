package com.freedy.tinyFramework.annotation.prop;

import java.lang.annotation.*;

/**
 * @author Freedy
 * @date 2021/12/7 10:10
 */
@Documented
@Target({ElementType.TYPE,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoneForce {

    /**
     * 该字段仅当标注在字段上面有效 <br/>
     * 普通类型和list类型为空时候的值(list的值用<b>逗号</b>分割),不指名的话默认为null
     */
    String normalTypeValIfNone() default "";

    /**
     * 该字段仅当标注在字段上面有效 <br/>
     * map类型为空时候的值,不指名的话默认为null
     */
    Node[] mapTypeValIfNone() default {};
}
