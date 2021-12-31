package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.LoopToken;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 11:01
 */
public class loopBuilder extends Builder {

    // for (i:10){// do some thing}
    private static final Pattern loopPattern = Pattern.compile("^for *?\\(([a-zA-Z_]\\w*) *?([:@])(.*?)\\) *?\\{(.*)}");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Node node = convertStr(token);
        if (node==null) return false;
        Matcher matcher = loopPattern.matcher(node.result);
        if (!matcher.find()) return false;
        LoopToken loopToken = new LoopToken(token);
        String variableName = matcher.group(1);
        if (StringUtils.isEmpty(variableName)) {
            holder.setMsg("loop variable can not be empty")
                    .setErrorPart("for in");
            return false;
        }
        loopToken.setVariableName(variableName);
        loopToken.setDesc(matcher.group(2).equals("@"));
        String executeEl = matcher.group(3);
        if ("REPLACE".equals(executeEl)) {
            if (StringUtils.isEmpty(node.aimedStr)) {
                holder.setMsg("executable part can not be empty")
                        .setErrorPart("in :");
                return false;
            }
            loopToken.setExecuteTokenStream(Tokenizer.doGetTokenStream(node.aimedStr));
        }

        loopToken.setLoopTokenStream(Tokenizer.doGetTokenStream(matcher.group(4)));

        tokenStream.addToken(loopToken);
        return true;
    }

    private Node convertStr(String token) {
        int length = token.length();
        char[] chars = token.toCharArray();
        int bracket = 0;
        int startIndex = -1;
        int endIndex = 0;
        for (; endIndex < length; endIndex++) {
            if (startIndex == -1) {
                if (chars[endIndex] == '@' || chars[endIndex] == ':') {
                    startIndex = endIndex + 1;
                } else continue;
            }

            if (chars[endIndex] == '(') {
                bracket++;
            }
            if (chars[endIndex] == ')') {
                if (--bracket == -1) {
                    break;
                }
            }
        }
        if (startIndex==-1||bracket != -1){
            return null;
        }
        return new Node(token.substring(0, startIndex) + "REPLACE" + token.substring(endIndex),
                token.substring(startIndex, endIndex).trim(), startIndex);
    }


    @ToString
    @AllArgsConstructor
    static class Node {
        String result;
        String aimedStr;
        int aimedIndex;
    }


    @Override
    public int priority() {
        return -1;
    }
}
