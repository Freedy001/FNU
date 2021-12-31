package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.Objects;

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
        Object variable = doRelevantOps(getVariable(), reference);
        if (variable == null) return null;
        return checkAndSelfOps(executeChain(variable.getClass(), variable, executableCount));
    }


    @Override
    public void assignFrom(Token assignment) {
        ExecuteStep step = getLastPropertyStep();
        Object variable = getVariable();
        if (variable == null) {
            throw new EvaluateException("there is no ? in the context", reference).errToken(this.errStr(reference));
        }
        if (step == null) {
            relevantAssign(
                    relevantOps,
                    () -> variable,
                    () -> assignment.calculateResult(ANY_TYPE),
                    () -> doAssign(assignment.calculateResult(ANY_TYPE))
            );
            return;
        }
        relevantAssign(
                step.getRelevantOps(),
                () -> executeChain(variable.getClass(), variable, executableCount,false),
                () -> assignment.calculateResult(ANY_TYPE),
                () -> doChainAssign(assignment, step, variable)
        );
    }


    private void doChainAssign(Token assignment, ExecuteStep step, Object variable) {
        variable = executeChain(variable.getClass(), variable, executableCount - 1);
        Type desiredType = Objects.requireNonNull(ReflectionUtils.getFieldRecursion(variable.getClass(), step.getPropertyName())).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(variable, step.getPropertyName(), result);
    }


    private void doAssign(Object result) {
        if (!context.containsVariable(reference)) {
            throw new EvaluateException("you must def ? first", reference);
        }
        context.setVariable(reference, result);
    }

    private Object getVariable() {
        if (StringUtils.isEmpty(reference)) {
            throw new EvaluateException("reference is null");
        }
        checkContext();
        return context.getVariable(reference);
    }
}
