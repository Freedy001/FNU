package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.token.Token;

import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/14 10:26
 */
public class ExpressionPasser {



    public Expression parseExpression(String expression) {


        return null;
    }


    public Expression parseExpression(String expression, ParserContext context) {

        return null;
    }


    /*
    #test.enabled=(T(com.freedy.Context).INTRANET_CHANNEL_RETRY_TIMES==#localProp.enabled)>{1,2,3,4,5}[0]
    T(com.freedy.Context).test()
    #localProp.enabled
    #localProp.init()
    #localProp?.enabled
    #localProp?.init()
    [1,2,3,4,5]
    {'k1':'123','k2':'321'}
    =
    ==
    <
    >
    ||
    &&
     */
    public List<Token> getTokenStream(String expression){

        return null;
    }

}
