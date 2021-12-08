package com.freedy.manage;

import com.freedy.log.LogRecorder;
import com.freedy.manage.entity.A;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;

/**
 * @author Freedy
 * @date 2021/12/3 17:48
 */
@Part
public class Test extends Response {

    @Inject
    private A a;

    @Inject
    private TestAspect testAspect;


    @Inject
    public void testInject(LogRecorder recorder) {
        System.out.println("inject method invoked");
        System.err.println(recorder);
    }


    public String testAspect(int a,long b,String c) {
        return a+b+c;
    }

    @PostConstruct
    public void testAspect() {
        testAspect.test();
    }
}
