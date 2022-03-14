package com.freedy.loadBalancing;


import com.freedy.Context;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * @author Freedy
 * @date 2021/11/16 20:09
 */
@Slf4j
@ToString
public abstract class LoadBalance<T> {
    protected List<T> lbElement = new CopyOnWriteArrayList<>();
    @Getter
    protected Object[] attribute;
    private static final Map<LoadBalance<?>, Runnable> shutdownTaskMap = new ConcurrentHashMap<>();
    private static volatile Thread sentinelThread;
    private List<Consumer<LoadBalance<T>>> eventList = new CopyOnWriteArrayList<>();
    private long lastZeroTime = 0;
    @Getter
    private boolean shutdownMode=false;


    @SafeVarargs
    public final void registerElementChangeEvent(Consumer<LoadBalance<T>>... event) {
        shutdownCheck();
        eventList.addAll(Arrays.asList(event));
    }


    /**
     * 当lbElement为空时，间隔一段时间执行传入的钩子函数 <br/>
     * 间隔时间由{@link Context#INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME}控制
     */
    public final void registerShutdownHook(Runnable runnable) {
        shutdownCheck();
        shutdownTaskMap.put(this, runnable);
        if (sentinelThread == null) {
            synchronized (shutdownTaskMap) {
                if (sentinelThread != null) return;
                //起一个线程专门用来回收所有LoadBalance实例
                sentinelThread = new Thread(() -> {
                    long sleepTime = Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME;
                    log.info("start load-balance-sentinel thread");
                    while (true) {
                        LockSupport.parkNanos(sleepTime);
                        sleepTime = Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME;
                        for (Map.Entry<LoadBalance<?>, Runnable> entry : shutdownTaskMap.entrySet()) {
                            LoadBalance<?> loadBalance = entry.getKey();
                            long lastZeroTime = loadBalance.lastZeroTime;
                            if (lastZeroTime == 0) continue;
                            long waitingTime = (now() - lastZeroTime) * 1_000_000_000;
                            //检测是否满足回收条件
                            if (waitingTime > Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME) {
                                //do hook
                                log.warn("ready to recycle load-balance{} and execute shutdown hook", loadBalance);
                                shutdownMode=true;
                                entry.getValue().run();
                                loadBalance.lbElement = null;
                                loadBalance.eventList = null;
                                loadBalance.attribute = null;
                                shutdownTaskMap.remove(loadBalance);
                            } else {
                                // 时间不够，算出最短时间
                                sleepTime = Math.min(Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME - waitingTime, sleepTime);
                                log.debug("ready to re-sleep on {} nanos",sleepTime);
                            }
                        }

                    }
                }, "load-balance-sentinel");
                sentinelThread.setDaemon(true);
                sentinelThread.start();
            }
        }
    }


    public int size() {
        shutdownCheck();
        return lbElement.size();
    }

    public void addElement(T element) {
        shutdownCheck();
        lbElement.add(element);
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
        if (sentinelThread != null && lastZeroTime != 0) {
            lastZeroTime = 0;
        }
    }

    public void removeElement(T element) {
        shutdownCheck();
        if (lbElement.remove(element) && lbElement.size() == 0 && sentinelThread != null) {
            lastZeroTime = now();
            LockSupport.unpark(sentinelThread);
        }
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
    }

    public T getElement() {
        shutdownCheck();
        if (lbElement.size() == 0) return null;
        return supply();
    }

    public void setElement(T[] element) {
        shutdownCheck();
        this.lbElement.clear();
        this.lbElement.addAll(Arrays.asList(element));
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
    }


    public List<T> getAllSafely() {
        shutdownCheck();
        return new ArrayList<>(lbElement);
    }

    public abstract T supply();

    public void setAttributes(Object... attr) {
        shutdownCheck();
        attribute = attr;
    }

    private long now() {
        return System.currentTimeMillis();
    }


    private void shutdownCheck() {
        if (shutdownMode) {
            throw new UnsupportedOperationException("LoadBalance is in shutdown mode!");
        }
    }
}
