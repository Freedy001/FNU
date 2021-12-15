package com.freedy.tinyFramework.Expression;

/**
 * @author Freedy
 * @date 2021/12/14 10:26
 */
public class ExpressionPasser {

    private final Tokenizer tokenizer = new Tokenizer();

    public Expression parseExpression(String expression) {
        return new Expression(tokenizer.getTokenStream(expression));
    }


    public Expression parseExpression(String expression, ParserContext context) {
        return null;
    }


}
