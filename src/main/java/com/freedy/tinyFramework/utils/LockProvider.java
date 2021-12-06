package com.freedy.tinyFramework.utils;

import com.freedy.tinyFramework.exception.BeanException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Freedy
 * @date 2021/12/4 17:49
 */
public class LockProvider {

    public static class initContainer{
        private static final AtomicReference<Thread> lockStatus= new AtomicReference<>();

        public static void enterCritical(){
            if (!lockStatus.compareAndSet(null,Thread.currentThread())){
                throw new BeanException("The bean container is initializing");
            }
        }

        public static void exitCritical(){
            if (!lockStatus.compareAndSet(Thread.currentThread(),null)){
                throw new BeanException("The bean container is initializing");
            }
        }

        public static boolean isInitialized(){
            return lockStatus.get()!=null;
        }
    }

}
