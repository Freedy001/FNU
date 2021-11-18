package com.freedy.loadBalancing;

/**
 * @author Freedy
 * @date 2021/11/16 20:19
 */
public class WeightedRoundRobin<T> extends LoadBalance<T>{

    WeightedRoundRobin() {
    }

    @Override
    public synchronized T supply() {
        return null;
    }
}
