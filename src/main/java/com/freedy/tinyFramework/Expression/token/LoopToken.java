package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

/**
 * @author Freedy
 * @date 2021/12/22 19:54
 */
@Getter
@Setter
@NoArgsConstructor
public final class LoopToken extends Token {
    //for i in 100:(// do some thing)
    private String variableName;
    // 100
    private TokenStream executeTokenStream;
    // (// do some thing)
    private TokenStream loopTokenStream;

    private Expression subExpression;

    public LoopToken(String value) {
        super("loop", value);
    }

    @Override
    public void setContext(EvaluationContext context) {
        super.setContext(context);
        subExpression = new Expression(context);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        subExpression.setTokenStream(executeTokenStream);
        Object iterable = subExpression.getValue();
        //准备执行loop
        subExpression.setTokenStream(loopTokenStream);
        Object result = null;
        if (iterable instanceof Number num) {
            for (long i = 0; i < num.longValue(); i++) {
                context.setVariable(variableName, i);
                result=subExpression.getValue();
            }
            return result;
        }
        if (iterable instanceof Object[] array) {
            iterable = Arrays.asList(array);
        }
        if (iterable instanceof Iterable collection) {
            for (Object o : collection) {
                context.setVariable(variableName, o);
                result=subExpression.getValue();
            }
            return result;
        }
        //非可迭代元素
        context.setVariable(variableName, iterable);
        result=subExpression.getValue();


        return result;
    }
}
