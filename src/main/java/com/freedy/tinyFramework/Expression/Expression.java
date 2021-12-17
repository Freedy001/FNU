package com.freedy.tinyFramework.Expression;

import com.alibaba.fastjson.JSON;
import com.freedy.tinyFramework.Expression.token.Assignable;
import com.freedy.tinyFramework.Expression.token.BasicVarToken;
import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author Freedy
 * @date 2021/12/14 11:18
 */
public class Expression {

    private final TokenStream stream;
    private final String expression;


    public Expression(TokenStream stream) {
        this.stream = stream;
        this.expression = stream.getExpression();
    }


    public Object getValue() {
        return evaluate(Object.class, null);
    }

    public <T> T getValue(Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, null));
    }

    public Object getValue(EvaluationContext context){
        return evaluate(Object.class, context);
    }

    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, context));
    }


    private Object evaluate(Class<?> desired, EvaluationContext context) {
        stream.setEachTokenContext(context);
        List<Token> tokenList = stream.calculateSuffix();
        System.out.println("\n");
        tokenList.forEach(item -> System.out.println(JSON.toJSONString(item)));
        Stack<Token> varStack = new Stack<>();
        List<Token> list = new ArrayList<>();
        for (Token token : tokenList) {
            try {
                if (token.isType("operation")) {
                    list.add(token);
                    Token token1 = varStack.pop();
                    Token token2 = varStack.pop();
                    varStack.push(doEvaluate(token, token2, token1));
                    continue;
                }
                varStack.push(token);
            }catch (ExpressionSyntaxException e){
                throw e;
            }catch (EvaluateException e){
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            }catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
        }
        if (varStack.size() == 1) {
            Token token = varStack.pop();
            Object result = null;
            try {
                result = token.calculateResult(desired);
            } catch (ExpressionSyntaxException e) {
                throw e;
            } catch (EvaluateException e) {
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            } catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
            return result;
        }
        ExpressionSyntaxException.tokenThr(expression, list.toArray(Token[]::new));
        throw new IllegalArgumentException("unreachable statement");
    }


    private Token doEvaluate(Token opsToken, Token t1, Token t2) {
        switch (opsToken.getValue()) {
            case "=" -> {
                return assign(t1, t2);
            }
            case "||", "&&" -> {
                return logicOps(t1, t2, opsToken);
            }
            //比较运输
            case "<", "==", ">=", "<=", ">", "!=" -> {
                return compare(t1, t2, opsToken);
            }
            //数字原始
            case "+", "/=", "*=", "-=", "+=", "/", "*", "-" -> {
                return numOps(t1, t2, opsToken);
            }
            default -> throw new EvaluateException("unrecognized ops ?", opsToken.getValue());
        }
    }


    private Token assign(Token t1, Token t2) {
        if (t1 instanceof Assignable t1Token) {
            t1Token.assignFrom(t2);
        } else {
            throw new EvaluateException("illegal assign");
        }
        return t1;
    }

    private Token logicOps(Token t1, Token t2, Token ops) {
        return new BasicVarToken("bool", t1.logicOps(t2, ops.getValue()) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
    }


    private Token compare(Token t1, Token t2, Token ops) {
        switch (ops.getValue()) {
            case "<" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) < 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case ">" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) > 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "<=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) <= 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case ">=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) >= 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "==" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) == 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "!=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) != 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            default -> throw new EvaluateException("unrecognized type ?", ops.getValue());
        }
    }


    private Token numOps(Token t1, Token t2, Token ops) {
        return new BasicVarToken("numeric", t1.numSelfOps(t2, ops.getValue())).setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
    }


}
