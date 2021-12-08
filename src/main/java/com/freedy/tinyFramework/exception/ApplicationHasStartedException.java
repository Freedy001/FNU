package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/8 17:33
 */
public class ApplicationHasStartedException extends BeanException {

    public ApplicationHasStartedException(Throwable cause) {
        super(cause);
    }

    public ApplicationHasStartedException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
