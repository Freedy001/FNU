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
    @SuppressWarnings("unchecked")
    public synchronized T supply() {
        return (T)lbElement.get(index++ % elementSize);
    }

}
