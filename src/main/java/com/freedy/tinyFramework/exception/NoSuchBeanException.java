package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/4 10:24
 */
public class NoSuchBeanException extends BeanException {

    public NoSuchBeanException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }

}
