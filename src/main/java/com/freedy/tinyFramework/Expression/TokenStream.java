package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.token.OpsToken;
import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import lombok.Getter;

import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/14 19:46
 */
public class TokenStream {
    @Getter
    private final List<Token> infixExpression = new ArrayList<>();
    private final Set<String> doubleOps = Set.of("||", "&&", "!=", "==", ">=", "<=", "++", "--", "+=", "-=");
    private final Set<String> singleOps = Set.of("++", "--", "!");
    private final List<Set<String>> priorityOps = Arrays.asList(
            Set.of("=", ">", "<", "||", "&&", "==", "!=", ">=", "<="),
            Set.of("+", "-", "+=", "-="),
            Set.of("*", "/"),
            singleOps
    );//从上往下 优先级逐渐变大
    @Getter
    private final String expression;

    // b<a=2+3+(5*4/2)
    // ba2=
    // <+
    public TokenStream(String expression) {
        this.expression = expression;
    }

    public int opsPriority(String ops) {
        for (int i = 0; i < priorityOps.size(); i++) {
            if (priorityOps.get(i).contains(ops)) {
                return i;
            }
        }
        return -1;
    }


    /**
     * 合并双元操作
     */
    public boolean mergeOps(char currentOps) {
        int size = infixExpression.size();
        if (size == 0) return false;
        Token token = infixExpression.get(size - 1);
        if (token.isType("operation") && !token.isValue(")")) {
            String nOps = token.getValue() + currentOps;
            if (doubleOps.contains(nOps)) {
                token.setValue(nOps);
                return true;
            } else {
                if (nOps.matches("\\+\\+\\+|---|\\+\\+-|--\\+")) {
                    infixExpression.add(new OpsToken(nOps.substring(2)));
                    return true;
                }
                ExpressionSyntaxException.thr(expression,nOps);
            }
        }
        return false;
    }


    public void addBracket(boolean isLeft) {
        if (isLeft) {
            infixExpression.add(new OpsToken("("));
        } else {
            infixExpression.add(new OpsToken(")"));
        }
    }

    public void addToken(Token token) {
        infixExpression.add(token);
    }


    // b<a=2+3+(5*4/2)
    // ba2=
    // <=+
    public List<Token> calculateSuffix() {
        mergeSingleTokenOps();
        List<Token> suffixExpression = new ArrayList<>();
        Stack<Token> opsStack = new Stack<>();
        //扫描中缀
        for (Token token : infixExpression) {
            if (token.isType("operation")) {
                Token pop;
                if (token.isValue(")")) {
                    while (!(pop = opsStack.pop()).isValue("(")) {
                        suffixExpression.add(pop);
                    }
                    continue;
                }
                while (true) {
                    pop = opsStack.isEmpty() ? null : opsStack.peek();
                    if (pop != null && !token.isValue("(") && opsPriority(pop.getValue()) > opsPriority(token.getValue())) {
                        //opsStack中的优先级较大 --> 压不住要跳出来
                        suffixExpression.add(opsStack.pop());
                    } else {
                        opsStack.add(token);
                        break;
                    }
                }
            } else {
                suffixExpression.add(token);
            }
        }
        while (!opsStack.isEmpty()) {
            suffixExpression.add(opsStack.pop());
        }
        return suffixExpression;
    }


    private void mergeSingleTokenOps() {
        for (int i = 0; i < infixExpression.size(); i++) {
            Token token = infixExpression.get(i);
            String ops = token.getValue();
            try {
                if (token.isType("operation") && singleOps.contains(ops)) {
                    if ("!".equals(ops)) {
                        if (i + 1 >= infixExpression.size()) {
                            ExpressionSyntaxException.tokenThr(expression, token);
                        }
                        Token nextToken = infixExpression.get(i + 1);
                        if (nextToken.isType("operation")) {
                            ExpressionSyntaxException.tokenThr(expression, token, nextToken);
                        }
                        nextToken.setNotFlag(true);
                        infixExpression.remove(i);
                    } else {
                        Token preToken = null;
                        if (i - 1 >= 0) {
                            preToken = infixExpression.get(i - 1);
                        }
                        Token nextToken = null;
                        if (i + 1 < infixExpression.size()) {
                            nextToken = infixExpression.get(i + 1);
                        }
                        if (preToken == null || preToken.isType("operation")) {
                            if (nextToken == null) {
                                ExpressionSyntaxException.tokenThr(expression, token);
                            }
                            assert nextToken != null;
                            if (ops.equals("++"))
                                nextToken.setPreSelfAddFlag(true);
                            else
                                nextToken.setPreSelfSubFlag(true);
                            infixExpression.remove(i);
                            continue;
                        }
                        if (nextToken == null || nextToken.isType("operation")) {
                            if (ops.equals("++"))
                                preToken.setPostSelfAddFlag(true);
                            else
                                preToken.setPostSelfSubFlag(true);
                            infixExpression.remove(i);
                        }
                    }
                }
            } catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e.getMessage(), expression, token);
            }
        }

    }

    public Token getToken(int index) {
        return infixExpression.get(index);
    }

}
