package com.freedy.tinyFramework.exception;

/**
 * @author Freedy
 * @date 2021/12/23 21:11
 */
public class StopSignal extends RuntimeException {

    String singular;

    public StopSignal(String singular) {
        super(singular);
        this.singular=singular;
    }

    public static StopSignal getInnerSignal(Throwable e) {
        if (e instanceof StopSignal signal) return signal;
        if (e == null) return null;
        return getInnerSignal(e.getCause());
    }

}
