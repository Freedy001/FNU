package com.freedy.manage.exception;

import lombok.Getter;

/**
 * @author Freedy
 * @date 2021/11/30 12:28
 */
@Getter
public class ErrorMsgException extends RuntimeException {

    private int code = 500;

    public ErrorMsgException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public ErrorMsgException(String msg) {
        super(msg);
    }

    public ErrorMsgException(Throwable e) {
        super(e);
    }

    public void printStackTrace() {
        Throwable cause = getCause();
        if (cause != null)
            cause.printStackTrace();
        else
            super.printStackTrace();
    }
}
