package com.freedy.manage;

import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.interceptor.Aspect;
import com.freedy.tinyFramework.annotation.interceptor.Pre;
import com.freedy.tinyFramework.processor.InterceptorOperation;

/**
 * @author Freedy
 * @date 2021/12/7 18:42
 */
@Aspect
public class TestAspect {

    @Inject
    private Test test;

    @Pre(interceptEL = "com.freedy.manage.Test.*(*)")
    public Object before1(InterceptorOperation operation){
        System.out.println(operation.getTargetMethod().getName());
        for (Object arg : operation.getTargetArgs()) {
            System.out.println(arg);
        }
        return "before1 return";
    }


    public void test(){
        test.okhaha("nihao",12,test,false);
    }
}
