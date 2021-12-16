package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;

import java.lang.reflect.Field;

/**
 * @author Freedy
 * @date 2021/12/15 14:06
 */
public class NormalVarToken extends Token implements Assignable {

    public NormalVarToken(String value) {
        super("normalVar", value);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        checkContext();
        Object root = context.getRoot();
        return checkAndSelfOps(ReflectionUtils.getter(root, value));
    }


    @Override
    public void assignFrom(Token assignment) {
        try {
            checkContext();
            if (StringUtils.isEmpty(value)) {
                throw new EvaluateException("can not assign! because null value").errStr(value+"=");
            }
            Object root = context.getRoot();
            Class<?> rootClass = root.getClass();
            Field field = ReflectionUtils.getFieldRecursion(rootClass, value);
            field.setAccessible(true);
            field.set(root, assignment.calculateResult(field.getGenericType()));
        } catch (Exception e) {
            throw new EvaluateException("assign failed,because ?", e);
        }
    }
}
