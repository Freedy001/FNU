package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.ObjectToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:43
 */
public class DefBuilder extends Builder {

    private static final Pattern defPattern = Pattern.compile("^def +?(.*)");


    @Override
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建def token
        Matcher matcher = defPattern.matcher(token);
        if (!matcher.find()) return false;
        ObjectToken objectToken = new ObjectToken(token);
        String varName = matcher.group(1);
        if (!varPattern.matcher(varName).matches()) {
            holder.setMsg("illegal var name")
                    .setErrorPart("def@" + varName);
            return false;
        }
        objectToken.setVariableName(varName);
        tokenStream.addToken(objectToken);
        return true;
    }

    @Override
    int priority() {
        return -1;
    }
}
