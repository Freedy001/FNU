package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Getter
@Setter
@NoArgsConstructor
@JSONType(includes = {"type", "value", "nullCheck", "propertyName", "methodArgs"})
public final class StaticToken extends ClassToken {

    public StaticToken(String value) {
        super("static", value);
    }

    @Override
    public Object doCalculate(Class<?> desiredType) {
        Class<?> type = getOpsType();
        return type == null ? null : checkAndSelfOps(executeChain(type, null, executableCount));
    }

    @Override
    public void assignFrom(Token assignment) {
        ExecuteStep step = getLastPropertyStep();
        if (step == null) {
            throw new EvaluateException("T(?) can not be assigned", reference).errToken(this.errStr(reference));
        }
        Class<?> type = getOpsType();
        if (type == null) {
            throw new EvaluateException("can not find class ?", reference).errToken(this.errStr(reference));
        }
        relevantAssign(
                step.getRelevantOps(),
                () -> executeChain(type, null, executableCount,false),
                () -> assignment.calculateResult(ANY_TYPE),
                () -> doChainAssign(assignment, step, type)
        );
    }

    private void doChainAssign(Token assignment, ExecuteStep step, Class<?> type) {
        Object variable = executeChain(type, null, executableCount - 1);
        type = variable == null ? type : variable.getClass();
        Type desiredType = ReflectionUtils.getFieldRecursion(type, step.getPropertyName()).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(type, variable, step.getPropertyName(), result);
    }

    private Class<?> getOpsType() {
        if (StringUtils.isEmpty(reference)) {
            throw new EvaluateException("reference is null,can not doCalculate");
        }
        try {
            return Class.forName(reference.replaceAll(" ",""));
        } catch (ClassNotFoundException e) {
            if (Optional.ofNullable(getRelevantOps()).orElse("").trim().equals("?")) {
                return null;
            }
            throw new EvaluateException("can not find class ?", e).errToken(this.errStr(reference));
        }
    }
}
