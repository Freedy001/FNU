package com.freedy.tinyFramework.Expression;

/**
 * @author Freedy
 * @date 2021/12/14 11:15
 */
public interface ParserContext {
    ParserContext STANDARD_Context =new ParserContext() {
        @Override
        public String getExpressionPrefix() {
            return "#{";
        }

        @Override
        public String getExpressionSuffix() {
            return "}";
        }
    };

    String getExpressionPrefix();


    String getExpressionSuffix();
}
