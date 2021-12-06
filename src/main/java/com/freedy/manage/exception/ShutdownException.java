package com.freedy.manage.exception;

/**
 * @author Freedy
 * @date 2021/11/29 20:33
 */
public class ShutdownException extends RuntimeException{

    private final Exception e;



    public ShutdownException(Exception e) {
        super(e);
        this.e=e;
    }

    public void printStackTrace(){
        e.printStackTrace();
    }
}
