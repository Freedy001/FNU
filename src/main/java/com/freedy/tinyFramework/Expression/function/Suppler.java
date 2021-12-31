package com.freedy.tinyFramework.Expression.function;

/**
 * @author Freedy
 * @date 2021/12/24 17:46
 */
@FunctionalInterface
public interface Suppler<T> extends Functional {
    T supply()throws Exception;
}
