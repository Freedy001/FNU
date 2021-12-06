package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/3 17:34
 */
public class NoUniqueBeanException extends BeanException{

    public NoUniqueBeanException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
