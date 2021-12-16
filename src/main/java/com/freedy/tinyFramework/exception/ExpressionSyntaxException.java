package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.utils.PlaceholderParser;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Freedy
 * @date 2021/12/15 9:49
 */
public class ExpressionSyntaxException extends RuntimeException {

    public static void tokenThr(String expression, Token... tokens) {
        tokenThr(":)syntax error", expression, tokens);
    }

    public static void tokenThr(String msg, String expression, Token... tokens) {
        List<String> syntaxErrSubStr = new ArrayList<>();
        for (Token token : tokens) {
            if (token != null) {
                syntaxErrSubStr.add(token.getValue());
            }
        }
        thr(msg, expression, syntaxErrSubStr.toArray(String[]::new));
    }

    public static void tokenThr(Throwable cause, String expression, Token... tokens) {
        List<String> syntaxErrSubStr = new ArrayList<>();
        for (Token token : tokens) {
            if (token != null) {
                syntaxErrSubStr.add(token.getValue());
            }
        }
        thr(cause, expression, syntaxErrSubStr);
    }

    public static void thrEvaluateException(EvaluateException e, String expression, Token token) {
        List<String> list = e.getSyntaxErrSubStrList();
        if (list.size() > 0) {
            thr(e, expression, list);
        } else {
            tokenThr(e, expression, token);
        }
    }

    public static void thr(String expression, String... syntaxErrSubStr) {
        thr(":)syntax error", expression, syntaxErrSubStr);
    }

    public static void thr(Throwable cause, String expression, String[] syntaxErrSubStr) {
        throw new ExpressionSyntaxException(cause, null, expression, syntaxErrSubStr);
    }

    public static void thr(String msg, String expression, String[] syntaxErrSubStr) {
        throw new ExpressionSyntaxException(null, msg, expression, syntaxErrSubStr);
    }

    public static void thr(Throwable cause, String expression, List<String> syntaxErrSubStr) {
        throw new ExpressionSyntaxException(cause, null, expression, syntaxErrSubStr.toArray(String[]::new));
    }


    @SneakyThrows
    public ExpressionSyntaxException(Throwable c, String msg, String expression, String[] syntaxErrSubStr) {
        StringBuilder highlightExpression = new StringBuilder();
        StringBuilder underLine = new StringBuilder();
        TreeMap<Integer, int[]> map = new TreeMap<>();
        for (String s : syntaxErrSubStr) {
            int[] i = findIndex(expression, s);
            if (i == null) {
                highlightExpression.append(new PlaceholderParser("syntax error expression ? at ?", expression, syntaxErrSubStr));
                break;
            } else {
                //排序
                map.put(i[0], i);
            }
        }
        if (highlightExpression.isEmpty()) {
            int lastSplit = 0;
            for (int[] i : map.values()) {
                highlightExpression.append("\033[93m").append(expression, lastSplit, i[0]).append("\033[0;39m");
                highlightExpression.append("\033[91m").append(expression, i[0], i[1]).append("\033[0;39m");
                underLine.append(" ".repeat(i[0] - lastSplit)).append("^".repeat(i[1] - i[0]));
                lastSplit = i[1];
            }

            highlightExpression.append("\033[93m").append(expression.substring(lastSplit)).append("\033[0;39m");
        }

        String errorMessage = new PlaceholderParser("""
                                
                                
                    \033[93m? at:\033[0;39m
                        ?
                        \033[91m?\033[0;39m
                """, msg == null ? c.getMessage() : msg, highlightExpression, underLine
        ).toString();

        Class<Throwable> aClass = Throwable.class;
        //设置msg
        Field exceptionMsg;
        exceptionMsg = aClass.getDeclaredField("detailMessage");
        exceptionMsg.setAccessible(true);
        exceptionMsg.set(this, errorMessage);
        if (c != null) {
            Field cause = aClass.getDeclaredField("cause");
            cause.setAccessible(true);
            cause.set(this, c);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private int[] findIndex(String str, String substr) {
        substr = substr.trim();
        char[] chars = str.toCharArray();
        char[] subChars = substr.toCharArray();
        int len = chars.length;
        int subLen = subChars.length;
        for (int i = 0; i < len; i++) {
            if (chars[i] == ' ') continue;
            int start = i;
            for (int j = 0; j < subLen; j++, i++) {
                for (; chars[i] == ' '; i++) ;
                for (; subChars[j] == ' '; j++) ;
                if (chars[i] != subChars[j]) {
                    break;
                } else {
                    if (j == subLen - 1) {
                        return new int[]{start, i + 1};
                    }
                }
            }
        }
        return null;
    }



}
