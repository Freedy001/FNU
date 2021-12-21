package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.Expression.token.Token;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/16 9:39
 */
public class EvaluateException extends BeanException {

    @Getter
    private final List<Token> tokenList=new ArrayList<>();

    public EvaluateException(String msg) {
        super(msg);
    }

    public EvaluateException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }

    public EvaluateException errToken(Token ...syntaxErrSubStr) {
        tokenList.addAll(Arrays.asList(syntaxErrSubStr));
        return this;
    }
}