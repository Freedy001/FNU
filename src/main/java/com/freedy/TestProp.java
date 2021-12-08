package com.freedy;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NoneForce;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/12/6 22:21
 */
@Data
@InjectProperties("fnu.framework.test")
public class TestProp {
    @NoneForce(normalTypeValIfNone = "haha")
    private String testStr;
    @NoneForce(normalTypeValIfNone = "32")
    private int testInt;
    @NoneForce
    private long testLong;
    @NoneForce
    private boolean testBool;
    @NoneForce
    private char testChar;
    @NoneForce
    private byte testByte;
    @NoneForce
    private double testDouble;
    @NoneForce
    private float testFloat;
    @NoneForce
    private short testShort;
    @NoneForce
    private ConcurrentHashMap<String, String> testMap;
    @NoneForce
    private Queue<Long> testList;
    private TestObj testObj;

    @Data
    public static class TestObj {
        @NoneForce
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
