package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.exception.StopSignal;
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
            for (int i = 0; i < num.longValue(); i++) {
                try {
                    context.setVariable(variableName, i);
                    result = subExpression.getValue();
                } catch (Throwable e) {
                    StopSignal signal = StopSignal.getInnerSignal(e);
                    if (signal != null) {
                        String signalMsg = signal.getMessage();
                        if ("continue".equals(signalMsg)) {
                            continue;
                        }
                        if ("break".equals(signalMsg)) {
                            break;
                        }
                    } else {
                        throw e;
                    }
                }
            }
            return result;
        }
        if (iterable instanceof Object[] array) {
            iterable = Arrays.asList(array);
        }
        if (iterable instanceof Iterable collection) {
            for (Object o : collection) {
                try {
                    context.setVariable(variableName, o);
                    result = subExpression.getValue();
                } catch (Throwable e) {
                    StopSignal signal = StopSignal.getInnerSignal(e);
                    if (signal != null) {
                        String signalMsg = signal.getMessage();
                        if ("continue".equals(signalMsg)) {
                            continue;
                        }
                        if ("break".equals(signalMsg)) {
                            break;
                        }
                    } else {
                        throw e;
                    }
                }
            }
            return result;
        }
        //非可迭代元素
        try {
            context.setVariable(variableName, iterable);
            result = subExpression.getValue();
        } catch (Throwable e) {
            StopSignal signal = StopSignal.getInnerSignal(e);
            if (signal == null) {
                throw e;
            }
        }


        return result;
    }
}
