package com.freedy.tinyFramework.Expression;

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
        List<Token> tokenList = stream.calculateSuffix();
        Stack<Token> varStack = new Stack<>();
        List<Token> list = new ArrayList<>();
        for (Token token : tokenList) {
            try {
                if (token.isType("operation")) {
                    list.add(token);
                    Token token1 = varStack.pop();
                    Token token2 = varStack.pop();
                    varStack.push(doEvaluate(token.getValue(),token2, token1));
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
            }catch (ExpressionSyntaxException e){
                throw e;
            }catch (EvaluateException e){
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            }catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
            return result;
        }
        ExpressionSyntaxException.tokenThr(expression, list.toArray(Token[]::new));
        throw new IllegalArgumentException("unreachable statement");
    }


    private Token doEvaluate(String ops,Token t1,Token t2) {
        switch (ops) {
            case "=" -> {
                return assign(t1, t2);
            }
            case "<" -> {
                return compare(t1, t2, "<");
            }
            case ">" -> {
                return compare(t1, t2, ">");
            }
            case "<=" -> {
                return compare(t1, t2, "<=");
            }
            case ">=" -> {
                return compare(t1, t2, ">=");
            }
            case "==" -> {
                return compare(t1, t2, "==");
            }
            case "!=" -> {
                return compare(t1, t2, "!=");
            }
            case "||" -> {
                return or(t1, t2);
            }
            case "&&" -> {
                return and(t1, t2);
            }
            case "+" -> {
                return numOps(t1, t2, "+");
            }
            case "-" -> {
                return numOps(t1, t2, "-");
            }
            case "*" -> {
                return numOps(t1, t2, "*");
            }
            case "/" -> {
                return numOps(t1, t2, "/");
            }
            case "+=" -> {
                return numOps(t1, t2, "+=");
            }
            case "-=" -> {
                return numOps(t1, t2, "-=");
            }
            default -> {
                throw new EvaluateException("unrecognized ops ?", ops);
            }
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


    private Token compare(Token t1, Token t2, String type) {
        switch (type) {
            case "<" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) < 0) + "");
            }
            case ">" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) > 0) + "");
            }
            case "<=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) <= 0) + "");
            }
            case ">=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) >= 0) + "");
            }
            case "==" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) == 0) + "");
            }
            case "!=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) != 0) + "");
            }
            default -> throw new EvaluateException("unrecognized type ?", type);
        }
    }

    private Token or(Token t1, Token t2) {
        return new BasicVarToken("bool", t1.logicOps(t2, "||") + "");
    }

    private Token and(Token t1, Token t2) {
        return new BasicVarToken("bool", t1.logicOps(t2, "&&") + "");
    }

    private Token numOps(Token t1, Token t2, String type) {
        return new BasicVarToken("numeric", t1.numOps(t2, type) + "");
    }


}
