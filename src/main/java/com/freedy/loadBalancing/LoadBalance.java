package com.freedy.loadBalancing;


import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Freedy
 * @date 2021/11/16 20:09
 */
@ToString
public abstract class LoadBalance<T> {
    protected List<T> lbElement = new CopyOnWriteArrayList<>();
    @Getter
    protected Object[] attribute;


    public int size() {
        return lbElement.size();
    }

    public void setElement(T[] element) {
        this.lbElement.clear();
        this.lbElement.addAll(Arrays.asList(element));
    }

    public void addElement(T element) {
        this.lbElement.add(element);
    }

    public void removeElement(T element) {
        lbElement.remove(element);
    }

    public T getElement() {
        return supply();
    }


    public List<T> getAllSafely() {
        return new ArrayList<>(lbElement);
    }

    public abstract T supply();

    public void setAttributes(Object... attr) {
        attribute = attr;
    }
}
