package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/7 16:08
 */
public class IllegalExpressionException extends BeanException{
    public IllegalExpressionException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
