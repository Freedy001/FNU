package com.freedy.loadBalancing;


import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author Freedy
 * @date 2021/11/16 20:09
 */
@ToString
public abstract class LoadBalance<T> {
    protected List<T> lbElement = new CopyOnWriteArrayList<>();
    @Getter
    protected Object[] attribute;
    private final List<Consumer<LoadBalance<T>>> eventList = new CopyOnWriteArrayList<>();

    @SafeVarargs
    public final void registerElementChangeEvent(Consumer<LoadBalance<T>>... event) {
        eventList.addAll(Arrays.asList(event));
    }

    public int size() {
        return lbElement.size();
    }

    public void addElement(T element) {
        this.lbElement.add(element);
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
    }

    public void removeElement(T element) {
        lbElement.remove(element);
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
    }

    public T getElement() {
        if (lbElement.size() == 0) return null;
        return supply();
    }

    public void setElement(T[] element) {
        this.lbElement.clear();
        this.lbElement.addAll(Arrays.asList(element));
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
    }


    public List<T> getAllSafely() {
        return new ArrayList<>(lbElement);
    }

    public abstract T supply();

    public void setAttributes(Object... attr) {
        attribute = attr;
    }
}
