package com.freedy.tinyFramework.Expression.tokenBuilder;

import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.token.ClassToken;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Getter;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 9:27
 */
public abstract class Builder {


    public final Pattern methodPattern = Pattern.compile("(.*?)\\((.*)\\)");

    public final Pattern varPattern = Pattern.compile("^[a-zA-Z_]\\w*");

    public final Pattern relevantAccess = Pattern.compile("(.*?)((?:\\?|\\[.*]|\\? *?\\[.*])+)");

    abstract boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder);

    //优先级 越小越高
    abstract int priority();


    protected void buildExecuteChain(ClassToken token, String suffixStr, ExceptionMsgHolder holder) {

        //构建执行链
        String[] step = StringUtils.splitWithoutBracket(suffixStr, '(', ')', '.');
        for (int i = 0; i < step.length; i++) {
            String propOrMethodName = step[i].trim();
            //检测是否包含list access
            Matcher listMatcher = relevantAccess.matcher(propOrMethodName);
            String relevantOps = null;
            if (listMatcher.find()) {
                propOrMethodName = listMatcher.group(1).trim();
                relevantOps = listMatcher.group(2).trim();
            }

            Matcher matcher = methodPattern.matcher(propOrMethodName);
            if (matcher.find()) {
                String methodName = matcher.group(1).trim();
                if (!varPattern.matcher(methodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    holder.setMsg("illegal method name")
                            .setErrorPart(StringUtils.hasText(joiner.toString()) ? (joiner + ".@" + methodName) : methodName);
                    return;
                }
                String group = matcher.group(2);
                token.addMethod(relevantOps, methodName, StringUtils.hasText(group) ? StringUtils.splitWithoutBracket(group, new char[]{'{', '('}, new char[]{'}', ')'}, ',') : new String[0]);
            } else {
                if (!varPattern.matcher(propOrMethodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    holder.setMsg("illegal prop name")
                            .setErrorPart(joiner + ".@" + propOrMethodName);
                }
                token.addProperties(relevantOps, propOrMethodName);
            }
        }

    }


    @Getter
    public static class ExceptionMsgHolder {
        boolean isErr = false;
        String msg;
        String[] errPart;

        public ExceptionMsgHolder setMsg(String msg) {
            isErr = true;
            this.msg = msg;
            return this;
        }

        public ExceptionMsgHolder setErrorPart(String... errPart) {
            isErr = true;
            this.errPart = errPart;
            return this;
        }
    }


}
