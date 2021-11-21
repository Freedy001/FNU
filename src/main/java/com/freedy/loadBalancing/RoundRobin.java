package com.freedy.loadBalancing;

/**
 * @author Freedy
 * @date 2021/11/16 20:07
 */
public class RoundRobin<T> extends LoadBalance<T> {

    private int index = 0;


    RoundRobin() {
    }

    @Override
    public T supply() {
        return lbElement.get(index++ % lbElement.size());
    }

}
