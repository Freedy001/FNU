package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.exception.IllegalArgumentException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Freedy
 * @date 2021/12/15 16:22
 */
@Data
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class StanderEvaluationContext implements EvaluationContext {

    private Object root;
    private Map<String, Object> variableMap = new HashMap<>();
    private Map<String, Function<?, ?>> funMap = new HashMap<>();

    public StanderEvaluationContext(Object root) {
        this.root = root;
    }


    public Object setRoot(Object root) {
        this.root = root;
        return root;
    }

    @Override
    public Object setVariable(String name, Object variable) {
        if (name.equals("root")) {
            root = variable;
        }
        return variableMap.put(name, variable);
    }

    @Override
    public Object getVariable(String name) {
        try {
            if (name.equals("root")) {
                return root;
            }
            return variableMap.get(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("get variable ? failed,because ?",name, e);
        }
    }

    @Override
    public <T, U> Function<T, U> registerFunction(String name, Function<T, U> function) {
        return (Function<T, U>) funMap.put(name, function);
    }

    @Override
    public <T, U> Function<T, U> getFunction(String name) {
        return (Function<T, U>) funMap.get(name);
    }
}
