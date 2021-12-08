package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/7 18:01
 */
public class ProxyExecuteException extends BeanException{

    public ProxyExecuteException(Throwable cause) {
        super(cause);
    }

    public ProxyExecuteException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
