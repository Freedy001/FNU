package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.token.ClassToken;
import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;

import java.util.List;
import java.util.Stack;

/**
 * @author Freedy
 * @date 2021/12/14 11:18
 */
public class Expression {

    private TokenStream stream;
    private String expression;
    private EvaluationContext context;


    public Expression(TokenStream stream) {
        this.stream = stream;
        this.expression=stream.getExpression();
    }


    public Object getValue() {

        return null;
    }

    public <T> T getValue(Class<T> desiredResultType) {

        return null;
    }

    public Object getValue(EvaluationContext context){

        return null;
    }

    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) {

        return null;
    }


    public void setValue(EvaluationContext context, Object value) {

    }

    private Object evaluate() {
        List<Token> tokenList = stream.calculateSuffix();
        Stack<Token> varStack = new Stack<>();
        for (Token token : tokenList) {
            if (token.isType("operation")) {

                continue;
            }
            varStack.push(token);
        }
        return null;
    }


    private Token doEvaluate(String ops,Token t1,Token t2) {
        return null;
    }

    /*
       Set.of("=", ">", "<", "||", "&&", "==", "!=", ">=", "<="),
            Set.of("+", "-", "+=", "-="),
            Set.of("*", "/"),
            Set.of("++", "--")
     */

    private Token assign(Token t1,Token t2){
        if (t1 instanceof ClassToken ct){

        }else {
            ExpressionSyntaxException.tokenThr("illegal assign",expression,t1,t2);
        }
        return null;
    }

    private Token gt(Token t1,Token t2){
        return null;
    }

    private Token lt(Token t1,Token t2){
        return null;
    }

    private Token or(Token t1,Token t2){
        return null;
    }

    private Token and(Token t1,Token t2){
        return null;
    }

    private Token eq(Token t1,Token t2){
        return null;
    }

    private Token neq(Token t1,Token t2){
        return null;
    }

    private Token ge(Token t1,Token t2){
        return null;
    }

    private Token le(Token t1,Token t2){
        return null;
    }





}
