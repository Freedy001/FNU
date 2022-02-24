package com.freedy.tinyFramework.annotation.prop;

import java.lang.annotation.*;

/**
 * <h1> 让属性注入不在是严格模式，即属性不存在时不会报错。</h1>
 * <b>defaultVal属性仅当标注在字段上时才有效</b> <br/>
 * <p>
 * <h1>使用示例:</h1>
 * <pre>
 *     {@code
 *     @NoneForce(defaultVal = {
 *             @N(k = "localServerAddress", v = "127.0.0.1,127.0.0.2"),
 *             @N(k = "remoteIntranetAddress", v = "127.0.0.1,127.0.0.2"),
 *             @N(k = "remoteServerPort", v = "12,32"),
 *     })
 *     private List&lt;Config&gt; listTest;
 *     @NoneForce(defaultVal = {
 *             @N(k = "localServerAddress", v = "127.0.0.1"),
 *             @N(k = "remoteIntranetAddress", v = "127.0.0.1"),
 *             @N(k = "remoteServerPort", v = "12"),
 *     })
 *     private Config objTest;
 *     @NoneForce(defaultVal = {
 *             @N(k = "k1", v = "f"),
 *             @N(k = "k2", v = "f"),
 *             @N(k = "k3", v = "true"),
 *     })
 *     private Map&lt;String, Boolean&gt; mapTest;
 *     }
 * </pre>
 * <h3>config对象</h3>
 * <pre>
 *     {@code
 *     public static class Config{
 *         private String localServerAddress;
 *         private String remoteIntranetAddress;
 *         private int remoteServerPort;
 *     }
 *     }
 * </pre>
 * 如果没有在配置文件中书写相关属性，则最终生成的结果对应的json数据为:
 * <pre>
 * {
 *   "listTest": [
 *     {
 *       "localServerAddress": "127.0.0.1",
 *       "remoteIntranetAddress": "127.0.0.1",
 *       "remoteServerPort": 12
 *     },
 *     {
 *       "localServerAddress": "127.0.0.2",
 *       "remoteIntranetAddress": "127.0.0.2",
 *       "remoteServerPort": 32
 *     }
 *   ],
 *   "mapTest": {
 *     "k1": false,
 *     "k2": false,
 *     "k3": true
 *   },
 *   "objTest": {
 *     "localServerAddress": "127.0.0.1",
 *     "remoteIntranetAddress": "127.0.0.1",
 *     "remoteServerPort": 12
 *   }
 * }
 * </pre>
 *
 * @author Freedy
 * @date 2021/12/7 10:10
 */
@Documented
@Target({ElementType.TYPE,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonStrict {
    /**
     * 设置非基本类型的默认值，即配置文件中没有指定该字段对应的值时 <br/>
     * 可以使用defaultVal()来设置默认值。相关用法见类javaDoc文档。<br/>
     * collection类型的值和配置文件的规则一样，使用<b>逗号(,)</b>分割。
     */
    N[] defaultVal() default {};
}
