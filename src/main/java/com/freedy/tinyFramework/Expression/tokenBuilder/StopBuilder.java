package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.StopToken;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:45
 */
public class StopBuilder extends Builder {

    public static final Pattern returnPattern = Pattern.compile("^return +(.*)");


    @Override
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建return
        Matcher matcher = returnPattern.matcher(token);
        if (matcher.find()) {
            StopToken stopToken = new StopToken(token);
            String group = matcher.group(1);
            if (StringUtils.hasText(group)) {
                stopToken.setReturnStream(Tokenizer.doGetTokenStream(group));
            }
            tokenStream.addToken(stopToken);
            return true;
        }
        //构建break
        if (token.matches("break|continue")) {
            tokenStream.addToken(new StopToken(token));
            return true;
        }
        return false;
    }

    @Override
    int priority() {
        return -1;
    }
}
