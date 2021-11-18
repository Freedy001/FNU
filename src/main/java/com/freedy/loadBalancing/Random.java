package com.freedy.loadBalancing;

/**
 * @author Freedy
 * @date 2021/11/16 20:13
 */

public class Random<T> extends LoadBalance<T> {

    Random() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized T supply() {
        return (T)lbElement.get((int) (Math.random() * 65025) % elementSize);
    }
}
