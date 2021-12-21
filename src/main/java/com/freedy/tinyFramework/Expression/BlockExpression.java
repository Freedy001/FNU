package com.freedy.tinyFramework.Expression;

import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/21 23:04
 */
public class BlockExpression {

    private final List<TokenStream> tokenStreamList;
    private final Expression expression;
    private final StanderEvaluationContext defaultContext=new StanderEvaluationContext();

    public BlockExpression(List<TokenStream> tokenStreamList) {
        expression = new Expression(tokenStreamList.remove(0));
        this.tokenStreamList = tokenStreamList;
    }

    public void execute() {
        while (!tokenStreamList.isEmpty()) {
            expression.getValue(defaultContext);
            expression.setTokenStream(tokenStreamList.remove(0));
        }
    }


    public void execute(EvaluationContext context) {
        while (!tokenStreamList.isEmpty()) {
            expression.getValue(context);
            expression.setTokenStream(tokenStreamList.remove(0));
        }
    }


}
