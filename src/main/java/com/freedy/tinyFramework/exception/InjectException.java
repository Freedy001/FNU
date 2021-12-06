package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/4 14:28
 */
public class InjectException extends BeanException{
    public InjectException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
