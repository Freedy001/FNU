package com.freedy.tinyFramework.annotation.prop;

import java.lang.annotation.*;

/**
 * <h1>属性注入</h1>
 * 将配置文件的属性注入到标有该注解的类，注入方式为 <b>前缀+字段名</b>  <br/>
 * 例如: 下面属性实体类,其声明的前缀为 fnu.framework.test
 * <pre>
 * {@code
 *     @Data
 *     @InjectProperties("fnu.framework.test")
 *     public class TestProp {
 *         private String testStr;
 *         private int testInt;
 *         private long testLong;
 *         private boolean testBool;
 *         private char testChar;
 *         private byte testByte;
 *         private double testDouble;
 *         private float testFloat;
 *         private short testShort;
 *         private ConcurrentHashMap<String, String> testMap;
 *         private Queue<Long> testList;
 *         private TestObj testObj;
 *
 *         @Data
 *         public static class TestObj{
 *             private String testStr;
 *             private int testInt;
 *             private long testLong;
 *             private boolean testBool;
 *             private char testChar;
 *             private byte testByte;
 *             private double testDouble;
 *             private float testFloat;
 *             private short testShort;
 *             private Map<String, String> testMap;
 *             private List<String> testList;
 *         }
 *
 *     }
 * }
 * 下面配置文件会被赋值到上面的实体类中
 * #测试基本类型
 * fnu.framework.test.testStr=haha
 * fnu.framework.test.testInt=12
 * fnu.framework.test.testLong=231231231321
 * fnu.framework.test.testBool=true
 * fnu.framework.test.testChar=A
 * fnu.framework.test.testByte=12
 * fnu.framework.test.testDouble=542.123
 * fnu.framework.test.testFloat=23.1
 * fnu.framework.test.testShort=2233
 * #测试Map类型
 * fnu.framework.test.testMap.k1=testMap.k1
 * fnu.framework.test.testMap.k2=2233.k1
 * fnu.framework.test.testMap.k3=testMap.123
 * #测试List类型
 * fnu.framework.test.testList=32,1,5,123123,11111,12,123
 * #测试嵌套类型
 * fnu.framework.test.testObj.testStr=12asd
 * fnu.framework.test.testObj.testInt=32
 * fnu.framework.test.testObj.testLong=12421415132312
 * fnu.framework.test.testObj.testBool=false
 * fnu.framework.test.testObj.testChar=B
 * fnu.framework.test.testObj.testByte=33
 * fnu.framework.test.testObj.testDouble=123.123
 * fnu.framework.test.testObj.testFloat=12.3123
 * fnu.framework.test.testObj.testShort=61
 * fnu.framework.test.testObj.testMap.k1=testObj
 * fnu.framework.test.testObj.testMap.k2=testByte
 * fnu.framework.test.testObj.testMap.k3=231231231321
 * fnu.framework.test.testObj.testList=abc,cd,ee,as,ff
 * </pre>
 * <h2>赋值规则</h2>
 * 基本类型和集合类型: 找到配置文件中符合(属性实体类声明前缀+字段名)的属性键的值,集合类型的值则是使用<b>(,)</b>隔开,进行赋值。 <br/>
 * map类型: 在配置文件中找到属性键的前缀为(属性实体类声明前缀+字段名)的属性,然后取该前缀(点)后面的一段属性名作为map的键,其值作为map的值 <br/>
 * 内嵌类型: 找到配置文件中符合(属性实体类声明前缀+内嵌字段的字段名+内嵌对象成员变量的名称)的属性键的值进行赋值。 <br/>
 *
 * <h2>非强制赋值</h2>
 * 默认赋值规则为强制赋值,即如果声明的字段在配置文件中找不到就会抛异常。
 * 可以使用{{@link NonStrict}}注解，禁用非强制赋值
 *
 *
 * @author Freedy
 * @date 2021/12/5 16:01
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectProperties {
    /**
     * 属性前缀
     */
    String value();

    /**
     * bean名称
     */
    String beanName() default "";

    /**
     * 指定需要被排除的字段
     */
    String[] exclude() default {};

    /**
     * 指定那个字段要被排除
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Exclude {
        /**
         * 如果标注的字段是一个非基本类型则可以使用此属性对子对象相关的字段进行排除， <br/>
         * <b>注意！如果标有此注解的字段没有指名exclude()则整个字段将被排除</b>
         */
        String[] exclude() default {};
    }


    /**
     * 当需要被属性注入的bean没有一条数据被注入时不对该bean放入容器
     */
    boolean nonePutIfEmpty() default false;
}
