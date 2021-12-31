package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.BasicVarToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:46
 */
public class BasicBuilder extends Builder{


    private static final Pattern strPattern = Pattern.compile("^'(.*?)'$");

    private static final Pattern numericPattern = Pattern.compile("\\d+|\\d+[lL]|\\d+?\\.\\d+");

    private static final Pattern boolPattern = Pattern.compile("^true$|^false$");

    @Override
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建 numeric Token
        Matcher matcher = numericPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("numeric", token);
            tokenStream.addToken(numeric);
            return true;
        }
        //构建string Token
        matcher = strPattern.matcher(token);
        if (matcher.find()) {
            BasicVarToken numeric = new BasicVarToken("str", matcher.group(1));
            tokenStream.addToken(numeric);
            return true;
        }
        //构建boolean Token
        matcher = boolPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("bool", token);
            tokenStream.addToken(numeric);
            return true;
        }
        return false;
    }

    @Override
    int priority() {
        return -1;
    }
}
