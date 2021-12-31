package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.Expression.TokenStream;
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
    @Getter
    private String expression;
    @Getter
    private String[] errPart;

    public EvaluateException(String msg) {
        super(msg);
    }

    public EvaluateException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }

    public EvaluateException subExpression(String expression){
        this.expression=expression;
        return this;
    }

    public EvaluateException errToken(Token ...syntaxErrSubStr) {
        tokenList.addAll(Arrays.asList(syntaxErrSubStr));
        return this;
    }

    public EvaluateException errorPart(String ...part){
        this.errPart=part;
        return this;
    }

    public EvaluateException tokenStream(TokenStream stream){
        this.expression=stream.getExpression();
        tokenList.addAll(stream.getAllTokens());
        return this;
    }
}
