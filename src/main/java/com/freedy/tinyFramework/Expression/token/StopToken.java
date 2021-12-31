package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.StopSignal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/23 20:57
 */
@Getter
@Setter
@NoArgsConstructor
public final class StopToken extends Token {

    TokenStream returnStream;

    public StopToken(String value) {
        super("keyword", value);
        if (!value.matches("break|continue|return.*")) {
            throw new EvaluateException("StopToken's value must be break or continue or return sth");
        }
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        throw new StopSignal(value).setReturnStream(returnStream);
    }
}
