package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.exception.EvaluateException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/23 21:43
 */
public final class IfToken extends Token {
    private final List<ExecuteUnit> executeUnits = new ArrayList<>();
    @Setter
    private TokenStream elseTokenStream;
    private Expression expression;

    public IfToken(String value) {
        super("if", value);
    }

    @Override
    public void setContext(EvaluationContext context) {
        super.setContext(context);
        expression = new Expression(context);
    }

    public void addStatement(TokenStream boolTokenStream, TokenStream trueTokenStream) {
        executeUnits.add(new ExecuteUnit(boolTokenStream, trueTokenStream));
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        for (ExecuteUnit unit : executeUnits) {
            TokenStream boolTokenStream = unit.getBoolTokenStream();
            expression.setTokenStream(boolTokenStream);
            Boolean value = expression.getValue(Boolean.class);
            if (value == null) {
                throw new EvaluateException("condition must return a bool value");
            }
            if (value) {
                TokenStream stream = unit.getTrueTokenStream();
                expression.setTokenStream(stream);
                return expression.getValue(ANY_TYPE);
            }
        }
        if (elseTokenStream != null) {
            expression.setTokenStream(elseTokenStream);
            return expression.getValue(ANY_TYPE);
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    static class ExecuteUnit {
        private TokenStream boolTokenStream;
        private TokenStream trueTokenStream;
    }
}
