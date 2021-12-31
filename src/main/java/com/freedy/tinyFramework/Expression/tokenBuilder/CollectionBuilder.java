package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.CollectionToken;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 10:08
 */
public class CollectionBuilder extends Builder {

    //[1,2,3,4,['test','banana'],'apple','haha'];
    private final Pattern withOps = Pattern.compile("^(?!\\{.*?}) *?\\[(.*)] *?\\[(.*)]");
    private final Pattern noneOps = Pattern.compile("^(?!\\{.*?}) *?\\[(.*)]()");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = withOps.matcher(token);
        if (!matcher.find()) {
            matcher = noneOps.matcher(token);
            if (!matcher.find()) {
                return false;
            }
        }

        CollectionToken collectionToken = new CollectionToken(token);
        String group = matcher.group(1);
        for (String ele : StringUtils.splitWithoutBracket(group, new char[]{'(', '{', '['}, new char[]{')', '}', ']'}, ',')) {
            String newEle = ele.trim();
            if (StringUtils.hasText(newEle)) {
                TokenStream stream = Tokenizer.doGetTokenStream(newEle);
                collectionToken.addTokenStream(stream);
            }
        }
        String ops = matcher.group(2);
        if (StringUtils.hasText(ops)) {
            collectionToken.setRelevantOps(Tokenizer.doGetTokenStream(ops));
        }
        tokenStream.addToken(collectionToken);
        return true;
    }


    @Override
    public int priority() {
        return 0;
    }

}
