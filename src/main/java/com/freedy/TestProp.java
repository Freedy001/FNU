package com.freedy;

import com.freedy.tinyFramework.annotation.beanContainer.InjectProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/12/6 22:21
 */
@Data
@InjectProperties("fnu.framework.test")
public class TestProp {
    private String testStr;
    private int testInt;
    private long testLong;
    private boolean testBool;
    private char testChar;
    private byte testByte;
    private double testDouble;
    private float testFloat;
    private short testShort;
    private Map<String, String> testMap;
    private List<String> testList;
    private TestObj testObj;

    @Data
    public static class TestObj{
        private String testStr;
        private int testInt;
        private long testLong;
        private boolean testBool;
        private char testChar;
        private byte testByte;
        private double testDouble;
        private float testFloat;
        private short testShort;
        private Map<String, String> testMap;
        private List<String> testList;
    }

}
