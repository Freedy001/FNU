package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.Expression.token.Token;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/16 9:39
 */
public class EvaluateException extends BeanException {

    @Getter
    private final List<String> syntaxErrSubStrList =new ArrayList<>();


    public EvaluateException(String msg) {
        super(msg);
    }

    public EvaluateException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }

    public EvaluateException errStr(String syntaxErrSubStr){
        this.syntaxErrSubStrList.add(syntaxErrSubStr);
        return this;
    }
    public EvaluateException errToken(Token syntaxErrSubStr){
        this.syntaxErrSubStrList.add(syntaxErrSubStr.getValue());
        return this;
    }

}
