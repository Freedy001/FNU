package com.freedy.tinyFramework.processor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * @author Freedy
 * @date 2021/12/7 17:06
 */
public class InterceptorOperation {
    @Getter
    private final Method targetMethod;
    @Getter
    private final Object[] targetArgs;
    @Getter
    private final Object tartBeanObj;

    @Setter(AccessLevel.PACKAGE)
    private Object lastReturnVal;
    @Setter(AccessLevel.PACKAGE)
    private String type;
    private final Queue<Supplier<Object>> executorChain = new ArrayDeque<>();


    public InterceptorOperation(Method method, Object[] args, Object bean) {
        this.targetMethod = method;
        this.targetArgs = args;
        this.tartBeanObj = bean;
    }

    public Object getLastReturn(){
        if (type.equals("com.freedy.tinyFramework.annotation.interceptor.Around"))
            throw new UnsupportedOperationException("getLastReturn() not support in Around method");
        return lastReturnVal;
    }

    void registerExecutorChain(Supplier<Object> executor) {
        executorChain.add(executor);
    }


    @SneakyThrows
    public Object invokeTarget() {
        if (!type.equals("com.freedy.tinyFramework.annotation.interceptor.Around"))
            throw new UnsupportedOperationException("invokeTarget() only support in Around method");
        Supplier<Object> executor = executorChain.poll();
        Object returnVal;
        if (executor != null) {
            returnVal = executor.get();
        } else {
            returnVal = targetMethod.invoke(tartBeanObj, targetArgs);
        }
        return returnVal;
    }


}
