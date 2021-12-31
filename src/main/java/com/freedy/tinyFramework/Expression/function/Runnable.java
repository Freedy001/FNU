package com.freedy.tinyFramework.Expression.function;

/**
 * @author Freedy
 * @date 2021/12/24 20:16
 */
@FunctionalInterface
public interface Runnable extends Functional {
    void run() throws Exception;
}
