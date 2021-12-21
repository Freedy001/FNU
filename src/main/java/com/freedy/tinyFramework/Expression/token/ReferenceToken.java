package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Getter
@Setter
@NoArgsConstructor
@JSONType(includes = {"type", "value", "nullCheck", "propertyName", "methodArgs"})
public final class ReferenceToken extends ClassToken {

    public ReferenceToken(String token) {
        super("reference", token);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        Object variable = getVariable();
        if (variable == null) return null;
        return checkAndSelfOps(executeChain(variable.getClass(), variable, executableCount));
    }


    @Override
    public void assignFrom(Token assignment) {
        String propertyName = getLastPropertyName();
        if (propertyName == null) {
            Object result = assignment.calculateResult(Token.ANY_TYPE);
            context.setVariable(reference, result);
            return;
        }
        Object variable = getVariable();
        if (variable == null) {
            throw new EvaluateException("there is no ? in the context", reference).errToken(this.errStr(reference));
        }
        variable = executeChain(variable.getClass(), variable, executableCount - 1);
        Type desiredType = ReflectionUtils.getFieldRecursion(variable.getClass(), propertyName).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(variable, propertyName, result);
    }

    private Object getVariable() {
        if (StringUtils.isEmpty(reference)) {
            throw new EvaluateException("reference is null");
        }
        checkContext();
        Object variable = context.getVariable(reference);
        if (!checkMode && variable == null) {
            throw new EvaluateException("there is no ? in the context", reference).errToken(this.errStr(reference));
        }
        return variable;
    }
}
