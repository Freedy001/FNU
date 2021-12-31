package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.StaticToken;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:37
 */
public class StaticBuilder extends Builder {

    private static final Pattern staticPattern = Pattern.compile("^T *?\\((.*?)\\) *(\\?)? *(\\.)? *?(.*)");

    @Override
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = staticPattern.matcher(token);
        if (!matcher.find()) return false;

        StaticToken staticToken = new StaticToken(token);
        staticToken.setReference(matcher.group(1).trim());
        staticToken.setRelevantOps(StringUtils.hasText(matcher.group(2)) ? "?" : null);
        //检测是否有点分字符
        if (StringUtils.hasText(matcher.group(3))) {
            //构建执行链
            buildExecuteChain(staticToken, matcher.group(4).trim(), holder);
            if (holder.isErr) return false;
        } else {
            holder.setMsg("static call must specify method or property")
                    .setErrorPart(staticToken.getReference() + "@)");
            return false;
        }
        tokenStream.addToken(staticToken);
        return true;
    }

    @Override
    int priority() {
        return 5;
    }
}
