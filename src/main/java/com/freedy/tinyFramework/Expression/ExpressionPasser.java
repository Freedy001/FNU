package com.freedy.tinyFramework.Expression;

/**
 * @author Freedy
 * @date 2021/12/14 10:26
 */
public class ExpressionPasser {



    public Expression parseExpression(String expression) {
        return new Expression(Tokenizer.getTokenStream(expression));
    }


    public Expression parseExpression(String expression, ParserContext context) {
        return null;
    }


}
