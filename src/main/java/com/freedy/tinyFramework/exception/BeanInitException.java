package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/4 21:57
 */
public class BeanInitException extends BeanException{
    public BeanInitException(Throwable cause) {
        super(cause);
    }

    public BeanInitException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
