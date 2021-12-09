package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/9 17:59
 */
public class IllegalArgumentException extends BeanException{
    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }

    public IllegalArgumentException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
