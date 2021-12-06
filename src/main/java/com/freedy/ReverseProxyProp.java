package com.freedy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/12/6 21:59
 */
@Slf4j
public class ReverseProxyProp {


    @SneakyThrows
    public static void main(String[] args) {
        Class<TestProp> aClass = TestProp.class;
        Class<?> obj = aClass.getDeclaredField("testObj").getType();
        Object o = obj.getConstructor().newInstance();
        System.out.println(o);
    }


}
