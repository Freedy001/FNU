package com.freedy.manage;

import com.freedy.tinyFramework.annotation.Value;
import com.freedy.tinyFramework.annotation.beanContainer.Part;

/**
 * @author Freedy
 * @date 2021/12/3 17:48
 */
@Part
public class Test extends Response {

    @Value("T(com.freedy.Context).INTRANET_CHANNEL_RETRY_TIMES")
    private TestAspect testAspect;


    public String testAspect(int a, long b, String c) {
        return a + b + c;
    }
}
