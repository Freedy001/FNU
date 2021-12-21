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
        String propertyName = getLastPropertyName();
        if (propertyName == null) {
            throw new EvaluateException("T(?) can not be assigned", reference).errToken(this.errStr(reference));
        }
        Class<?> type = getOpsType();
        if (type == null) {
            throw new EvaluateException("can not find class ?", reference).errToken(this.errStr(reference));
        }
        Object variable = executeChain(type, null, executableCount - 1);
        type = variable == null ? type : variable.getClass();
        Type desiredType = ReflectionUtils.getFieldRecursion(type, propertyName).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(type, variable, propertyName, result);
    }

    private Class<?> getOpsType() {
        if (StringUtils.isEmpty(reference)) {
            throw new EvaluateException("reference is null,can not doCalculate");
        }
        try {
            return Class.forName(reference);
        } catch (ClassNotFoundException e) {
            if (checkMode) {
                return null;
            }
            throw new EvaluateException("can not find class ?", e).errToken(this.errStr(reference));
        }
    }
}
