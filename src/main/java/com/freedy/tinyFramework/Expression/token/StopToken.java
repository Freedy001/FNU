package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.StopSignal;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Freedy
 * @date 2021/12/23 20:57
 */
@Getter
@NoArgsConstructor
public final class StopToken extends Token {

    public StopToken(String value) {
        super("keyword", value);
        if (!value.matches("break|continue")) {
            throw new EvaluateException("StopToken's value must be keyword");
        }
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        throw new StopSignal(value);
    }
}
