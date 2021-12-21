package com.freedy.tinyFramework.Expression;

import com.alibaba.fastjson.JSON;
import com.freedy.tinyFramework.Expression.token.*;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author Freedy
 * @date 2021/12/14 11:18
 */
public class Expression {
    private String expression;
    private TokenStream stream;
    @Getter
    @Setter
    private EvaluationContext defaultContext;

    public Expression(TokenStream stream) {
        this.stream = stream;
        this.expression = stream.getExpression();
    }

    public Expression(EvaluationContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public Expression(TokenStream stream, EvaluationContext defaultContext) {
        this.stream = stream;
        this.expression = stream.getExpression();
        this.defaultContext = defaultContext;
    }

    public void setTokenStream(TokenStream stream) {
        this.stream = stream;
        this.expression = stream.getExpression();
    }

    public Object getValue() {
        return evaluate(Token.ANY_TYPE, defaultContext);
    }

    public <T> T getValue(Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, defaultContext));
    }

    public Object getValue(EvaluationContext context) {
        return evaluate(Token.ANY_TYPE, context);
    }

    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, context));
    }


    private Object evaluate(Class<?> desired, EvaluationContext context) {
        stream.setEachTokenContext(context);
        List<Token> tokenList = stream.calculateSuffix();

        stream.getInfixExpression().forEach(item -> System.out.println(JSON.toJSONString(item)));
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
            } catch (ExpressionSyntaxException e) {
                ExpressionSyntaxException.thrThis(expression,e);
            } catch (EvaluateException e) {
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            } catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
        }
        if (varStack.size() == 1) {
            Token token = varStack.pop();
            Object result = null;
            try {
                result = token.calculateResult(desired);
            } catch (ExpressionSyntaxException e) {
                ExpressionSyntaxException.thrThis(expression,e);
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
            case "." -> {
                return mergeDotSplit(t1, t2, opsToken);
            }
            case "?" -> {
                return ternaryOps(t1, t2, opsToken);
            }
            case "=" -> {
                return assign(t1, t2, opsToken);
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

    private Token ternaryOps(Token t1, Token t2, Token opsToken) {
        if (t2 instanceof TernaryToken token) {
            token.setBoolToken(t1);
            return t2.setOriginToken(t1, t2).setOffset(t1.getOffset());
        }
        throw new EvaluateException("can not do ternary ops,because ? is not ternary token", t2.getValue()).errToken(opsToken).errToken(t2);

    }

    private Token mergeDotSplit(Token t1, Token t2, Token opsToken) {
        if (t2 instanceof DotSplitToken dotSplitToken) {
            dotSplitToken.setBaseToken(t1);
            return dotSplitToken.setOriginToken(t1, t2).setOffset(t1.getOffset());
        }
        throw new EvaluateException("can not merge dot split token,because ? is not dot split token", t2.getValue()).errToken(opsToken).errToken(t2);
    }


    private Token assign(Token t1, Token t2, Token opsToken) {
        if (t1 instanceof Assignable token) {
            token.assignFrom(t2);
            return t1.setOriginToken(t1, t2).setOffset(t1.getOffset());
        } else {
            throw new EvaluateException("illegal assign").errToken(t1, opsToken, t2);
        }
    }

    private Token logicOps(Token t1, Token t2, Token ops) {
        return new BasicVarToken("bool", t1.logicOps(t2, ops) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
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
                boolean flag = false;
                Object o1 = t1.calculateResult(Token.ANY_TYPE);
                Object o2 = t2.calculateResult(Token.ANY_TYPE);
                if (o1 != null && o2 != null) {
                    if (ReflectionUtils.isRegularType(o1.getClass()) && ReflectionUtils.isRegularType(o2.getClass())) {
                        flag = o1.equals(o2);
                    } else {
                        //至少有一个不是常规类型
                        flag = o1 == o2;
                    }
                } else if (o1 == null && o2 == null) {
                    flag = true;
                }
                return new BasicVarToken("bool", flag + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "!=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) != 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            default -> throw new EvaluateException("unrecognized type ?", ops.getValue());
        }
    }


    private Token numOps(Token t1, Token t2, Token ops) {
        return new BasicVarToken("numeric", t1.numSelfOps(t2, ops)).setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
    }


}
