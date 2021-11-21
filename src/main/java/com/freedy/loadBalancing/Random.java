package com.freedy.loadBalancing;

/**
 * @author Freedy
 * @date 2021/11/16 20:13
 */

public class Random<T> extends LoadBalance<T> {

    Random() {
    }

    @Override
    public T supply() {
        return lbElement.get((int) (Math.random() * 65025) % lbElement.size());
    }
}
