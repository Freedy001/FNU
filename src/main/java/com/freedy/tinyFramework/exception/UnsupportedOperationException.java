package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/14 19:48
 */
public class UnsupportedOperationException extends BeanException{
    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }

    public UnsupportedOperationException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
