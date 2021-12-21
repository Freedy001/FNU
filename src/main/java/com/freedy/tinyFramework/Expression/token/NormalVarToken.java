package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.NoArgsConstructor;

/**
 * @author Freedy
 * @date 2021/12/15 14:06
 */
@NoArgsConstructor
public final class NormalVarToken extends Token implements Assignable {

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
        checkContext();
        if (StringUtils.isEmpty(value)) {
            throw new EvaluateException("can not assign! because null value").errToken(this.getNextToken());
        }
        Object result = assignment.calculateResult(ANY_TYPE);
        ReflectionUtils.setter(context.getRoot(), value, result);
    }
}
