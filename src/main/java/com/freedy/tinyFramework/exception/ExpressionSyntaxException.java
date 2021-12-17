package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.utils.PlaceholderParser;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/15 9:49
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpressionSyntaxException extends RuntimeException {


    public static void tokenThr(String expression, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(Throwable cause, String expression, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildCause(cause)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thrEvaluateException(EvaluateException e, String expression, Token token) {
        List<Token> tokens = e.getTokenList();
        List<String> list = e.getSyntaxErrSubStrList();
        boolean noErrInfo = tokens.isEmpty() && list.isEmpty();
        new ExpressionSyntaxException(expression)
                .buildCause(e)
                .buildToken(noErrInfo ? new Token[]{token} : tokens.toArray(Token[]::new))
                .buildErrorStr(noErrInfo ? null : list.toArray(String[]::new))
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thr(String expression, String... syntaxErrSubStr) {
        new ExpressionSyntaxException(expression)
                .buildErrorStr(syntaxErrSubStr)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    private String msg;
    private Throwable cause;
    private List<List<Token>> layer;
    private String expression;
    private PlaceholderParser placeholder;
    private List<String> syntaxErrStr;
    private Map<Token, int[]> currentTokenIndex = new HashMap<>();

    public ExpressionSyntaxException(String expression) {
        this.expression = expression;
    }

    public ExpressionSyntaxException buildToken(Token... tokens) {
        if (tokens == null || tokens.length == 0) {
            layer = new ArrayList<>();
            return this;
        }

        List<List<Token>> list = new ArrayList<>();
        LinkedList<Token> queue = new LinkedList<>(Arrays.asList(tokens));
        queue.sort(Comparator.comparingInt(Token::getOffset));

        queue.add(null);
        List<Token> t = new ArrayList<>();
        while (!queue.isEmpty()) {
            Token poll = queue.poll();
            if (poll == null) {
                list.add(t);
                if (queue.isEmpty()) break;
                t = new ArrayList<>();
                queue.add(null);
                continue;
            }
            t.add(poll);
            List<Token> originToken = poll.getOriginToken();
            if (originToken != null && !originToken.isEmpty()) {
                for (Token origin : originToken) {
                    origin.setSonToken(poll);
                    queue.add(origin);
                }
            }
        }
        layer = new ArrayList<>();
        for (int i = list.size() - 1; i > 0; i--) {
            layer.add(list.get(i));
        }
        return this;
    }


    public ExpressionSyntaxException buildErrorStr(String... str) {
        if (str == null) return this;
        if (syntaxErrStr == null) syntaxErrStr = new ArrayList<>();
        for (String s : str) {
            if (StringUtils.hasText(s)) {
                syntaxErrStr.add(s);
            }
        }
        return this;
    }

    public ExpressionSyntaxException buildMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ExpressionSyntaxException buildCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public ExpressionSyntaxException buildConsoleErrorMsg() {
        List<SyntaxErr> syntaxErrSubStr = new ArrayList<>(layer.remove(0).stream().map(SyntaxErr::new).toList());
        syntaxErrSubStr.addAll((syntaxErrStr.stream().map(SyntaxErr::new).toList()));

        StringBuilder highlightExpression = new StringBuilder();
        StringBuilder underLine = new StringBuilder();
        TreeMap<Integer, int[]> map = new TreeMap<>();
        for (SyntaxErr s : syntaxErrSubStr) {
            int[] i = findIndex(expression, s);
            if (s.isTokenType()) {
                currentTokenIndex.put(s.getRelevantToken(), i);
            }
            if (i == null) {
                highlightExpression.append(new PlaceholderParser("expression: ? illegal at ?*", expression, syntaxErrSubStr).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN));
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

        placeholder = new PlaceholderParser("""
                            
                            
                \033[93m? at:\033[0;39m
                    ?
                    \033[91m?\033[0;39m""", msg == null ? (cause == null ? ":)syntax error" : cause.getMessage()) : msg, highlightExpression, underLine
        );
        return this;
    }

    public ExpressionSyntaxException buildStackTrace() {
        for (List<Token> tokenList : layer) {
            LinkedHashMap<Token, int[]> tokenIndex = new LinkedHashMap<>();
            tokenList.sort(Comparator.comparing(Token::getOffset));
            for (Token token : tokenList) {
                List<Token> originToken = token.getOriginToken();
                TreeMap<Integer, int[]> map = new TreeMap<>();
                for (Token origin : originToken) {
                    int[] ints = currentTokenIndex.get(origin);
                    map.put(ints[0], ints);
                }
                int midIndex = (map.firstKey() + map.lastEntry().getValue()[1]) / 2;
                int length = token.getValue().length();
                tokenIndex.put(token, new int[]{midIndex - length / 2, midIndex + (length - length / 2)});
            }
            StringBuilder builder = new StringBuilder();
            StringBuilder underLine = new StringBuilder();
            int[] lastSplit = {0};
            tokenIndex.forEach((k, v) -> {
                builder.append(" ".repeat(v[0] - lastSplit[0])).append(k.getValue());
                underLine.append(" ".repeat(v[0] - lastSplit[0])).append(k.getValue());
                lastSplit[0] = v[1];
            });
            builder.append(" ".repeat(expression.length() - lastSplit[0]));
            placeholder.join(true, """
                    \033[93m?\033[0;39m
                    \033[91m?\033[0;39m
                    """, builder, underLine);
        }
        return this;
    }


    @SneakyThrows
    public void thr() {
        Class<Throwable> aClass = Throwable.class;
        //设置msg
        Field exceptionMsg;
        exceptionMsg = aClass.getDeclaredField("detailMessage");
        exceptionMsg.setAccessible(true);
        exceptionMsg.set(this, placeholder.toString());
        if (cause != null) {
            Field cause = aClass.getDeclaredField("cause");
            cause.setAccessible(true);
            cause.set(this, cause);
        }
        throw this;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    private int[] findIndex(String str, SyntaxErr err) {
        String substr = err.info.trim();
        char[] chars = str.toCharArray();
        char[] subChars = substr.toCharArray();
        int len = chars.length;
        int subLen = subChars.length;
        int behindIndex = -1;
        for (int i = err.startIndex; i < len; i++) {
            if (chars[i] == ' ') continue;
            int start = i;
            for (int j = 0; j < subLen; j++, i++) {
                for (; chars[i] == ' '; i++) ;
                for (; subChars[j] == ' '; j++) ;
                if (subChars[j] == '@') {
                    start = i;
                    j++;
                }
                if (subChars[j] == '$') {
                    behindIndex = i;
                    j++;
                }
                if (chars[i] != subChars[j]) {
                    break;
                } else {
                    if (j == subLen - 1) {
                        return new int[]{start, behindIndex != -1 ? behindIndex : i + 1};
                    }
                }
            }
        }
        return null;
    }

    @Data
    static class SyntaxErr {
        String info;
        int startIndex;
        Token relevantToken;

        boolean isTokenType() {
            return relevantToken == null;
        }

        public SyntaxErr(String info) {
            this.info = info;
        }

        public SyntaxErr(Token token) {
            info = token.getValue();
            startIndex = token.getOffset();
            relevantToken = token;
        }
    }


}
