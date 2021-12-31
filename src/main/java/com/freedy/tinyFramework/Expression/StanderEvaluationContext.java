package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.function.Functional;
import com.freedy.tinyFramework.Expression.stander.StanderFunc;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/12/15 16:22
 */
@Data
@NoArgsConstructor
public class StanderEvaluationContext implements EvaluationContext {

    private String time = DateTimeFormatter.ofPattern("hh:mm:ss").format(LocalDateTime.now());
    private Object root;
    private Map<String, Object> variableMap = new HashMap<>();
    private Map<String, Functional> funMap = new HashMap<>();


    {
        new StanderFunc(this).registerStanderFunc();
    }


    public StanderEvaluationContext(Object root) {
        this.root = root;
    }


    public Object setRoot(Object root) {
        this.root = root;
        return root;
    }

    @Override
    public Object setVariable(String name, Object variable) {
        name = filterName(name);
        if (name.equals("root")) {
            root = variable;
        }
        return variableMap.put(name, variable);
    }

    @Override
    public Object getVariable(String name) {
        try {
            name = filterName(name);
            if (name.equals("root")) {
                return root;
            }
            return variableMap.get(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("get variable ? failed,because ?", name, e);
        }
    }

    @Override
    public boolean containsVariable(String name) {
        name = filterName(name);
        return variableMap.containsKey(name);
    }

    @Override
    public Object removeVariable(String name) {
        return variableMap.remove(name);
    }

    @Override
    public void clearVariable() {
        variableMap.clear();
    }


    public Functional registerFunction(String name, Functional function) {
        return funMap.put(name, function);
    }



    public Functional getFunction(String name) {
        return funMap.get(name);
    }

    @Override
    public boolean containsFunction(String funcName) {
        return funMap.containsKey(funcName);
    }

    @Override
    public Functional removeFunction(String name) {
        return funMap.remove(name);
    }

    @Override
    public void clearFunction() {
        funMap.clear();
    }
}
