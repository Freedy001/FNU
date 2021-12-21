package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.exception.EvaluateException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/21 16:58
 */
@Setter
@Getter
@NoArgsConstructor
public final class TernaryToken extends Token {
    private TokenStream trueTokenStream;
    private TokenStream falseTokenStream;
    private Token boolToken;

    public TernaryToken(String value) {
        super("ternary", value);
    }

    public void setBoolToken(Token boolToken) {
        this.boolToken = boolToken;
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        Boolean result = (Boolean) boolToken.calculateResult(Boolean.class);
        if (result == null) {
            throw new EvaluateException("? calculate a null value", boolToken.getValue()).errToken(boolToken);
        }
        if (result) {
            return checkAndSelfOps(new Expression(trueTokenStream).getValue(context));
        } else {
            return checkAndSelfOps(new Expression(falseTokenStream).getValue(context));
        }
    }
}
