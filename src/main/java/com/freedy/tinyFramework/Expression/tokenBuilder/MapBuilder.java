package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.MapToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 10:44
 */
public class MapBuilder extends Builder {

    //      static                                             {a:b,c:d} [2]
    private static final Pattern mapPattern = Pattern.compile("^(\\{.*?})(?: *?\\[(.*)])?");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = mapPattern.matcher(token);
        if (!matcher.find()) {
            return false;
        }
        MapToken mapToken = new MapToken(token);
        mapToken.setMapStr(matcher.group(1));
        mapToken.setRelevantOpsName(matcher.group(2));
        tokenStream.addToken(mapToken);
        return true;
    }


    @Override
    public int priority() {
        return -1;
    }
}
