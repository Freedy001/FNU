package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.function.Functional;

/**
 * @author Freedy
 * @date 2021/12/14 11:11
 */

public interface EvaluationContext {
    Object setVariable(String name, Object variable);

    Object getVariable(String name);

    boolean containsVariable(String name);

    Object removeVariable(String name);

    void clearVariable();

    Object setRoot(Object root);

    Object getRoot();

    Functional registerFunction(String name, Functional function);

    Functional getFunction(String name);

    boolean containsFunction(String funcName);

    Functional removeFunction(String name);

    void clearFunction();

    default String filterName(String name){
        if (name.matches("^[@#].*")){
            name=name.substring(1);
        }
        return name;
    }

}
