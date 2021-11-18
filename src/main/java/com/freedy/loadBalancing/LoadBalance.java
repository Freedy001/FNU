package com.freedy.loadBalancing;


import com.freedy.Struct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/11/16 20:09
 */
public abstract class LoadBalance<T> {
    protected List<Object> lbElement=new ArrayList<>();
    protected int elementSize;
    protected Object[] attribute;

    synchronized void loadAddress(String[] remoteAddress){
        int length = remoteAddress.length;
        this.lbElement=new ArrayList<>();
        this.elementSize=length;
        for (String address : remoteAddress) {
            String[] split = address.split(":");
            this.lbElement.add(new Struct.IpAddress(split[0], Integer.parseInt(split[1])));
        }
    }

    public synchronized void setElement(T[] element) {
        this.lbElement.clear();
        elementSize=0;
        this.lbElement.addAll(Arrays.asList(element));
    }

    public synchronized void addElement(T element){
        this.lbElement.add(element);
        elementSize++;
    }

    public synchronized void removeElement(T element){
        lbElement.remove(element);
        elementSize--;
    }

    public T getElement() {
        if (elementSize==0) return null;
        return supply();
    }

    public abstract T supply();

    public synchronized void setAttributes(Object ...attr){
        attribute=attr;
    }
}
