package com.freedy.tinyFramework.Expression;

import java.util.function.Function;

/**
 * @author Freedy
 * @date 2021/12/14 11:11
 */

public interface EvaluationContext {
    Object setVariable(String name,Object variable);
    Object getVariable(String name);
    Object setRoot(Object root);
    Object getRoot();
    <T,U> Function<T, U> registerFunction(String name, Function<T,U> function);
    <T,U> Function<T,U> getFunction(String name);
}
