package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.DirectAccessToken;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 11:42
 */
public class DirectAccessBuilder extends Builder {


    private final Pattern prefix = Pattern.compile("^([a-zA-Z_]\\w*) *(.*)");
    private final Pattern methodPattern = Pattern.compile("^([a-zA-Z_]\\w*) *?\\((.*)\\) *(.*)");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建变量 Token
        Matcher matcher = prefix.matcher(token);

        if (!matcher.find()) return false;
        String propOrMethod = matcher.group(1);
        if (StringUtils.isEmpty(propOrMethod)) {
            return false;
        }
        if (propOrMethod.equals("T")) {
            return false;
        }


        String[] splitWithoutBracket = StringUtils.splitWithoutBracket(token, '(', ')', '.', 2);

        DirectAccessToken directAccessToken = new DirectAccessToken(token);
        Matcher methodMatcher = methodPattern.matcher(splitWithoutBracket[0]);
        if (methodMatcher.find()) {
            //method
            directAccessToken.setMethodName(methodMatcher.group(1));
            directAccessToken.setMethodArgsName(StringUtils.splitWithoutBracket(methodMatcher.group(2), new char[]{'{', '('}, new char[]{'}', ')'}, ','));
            String suffix = methodMatcher.group(3);
            if (StringUtils.hasText(suffix = suffix.trim())) {
                directAccessToken.setRelevantOps(suffix);
            }
        } else {
            matcher = prefix.matcher(splitWithoutBracket[0]);
            if (matcher.find()) {
                directAccessToken.setVarName(matcher.group(1));
                String suffix = matcher.group(2);
                if (StringUtils.hasText(suffix = suffix.trim())) {
                    directAccessToken.setRelevantOps(suffix);
                }
            }else {
                holder.setMsg("illegal token");
                return false;
            }
        }


        if (splitWithoutBracket.length == 2) {
            buildExecuteChain(directAccessToken, splitWithoutBracket[1], holder);
            if (holder.isErr()) {
                return false;
            }
        }
        tokenStream.addToken(directAccessToken);
        return true;
    }

    @Override
    public int priority() {
        return 4;
    }
}
