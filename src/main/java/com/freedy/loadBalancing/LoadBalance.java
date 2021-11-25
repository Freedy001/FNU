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


    @SafeVarargs
    public final void registerElementChangeEvent(Consumer<LoadBalance<T>>... event) {
        eventList.addAll(Arrays.asList(event));
    }

    @SuppressWarnings("BusyWait")
    public final void registerShutdownHook(Runnable runnable) {
        shutdownTaskMap.put(this, runnable);
        if (sentinelThread == null) {
            synchronized (shutdownTaskMap) {
                if (sentinelThread != null) return;
                sentinelThread = new Thread(() -> {
                    long sleepTime = Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME;
                    log.info("start load-balance-sentinel thread");
                    while (true) {
                        LockSupport.park();
                        try {
                            System.out.println("睡眠时间：" + (sleepTime + 1000));
                            Thread.sleep(sleepTime + 1000);
                            sleepTime = Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME;
                        } catch (InterruptedException ignored) {
                            log.warn("Thread sleep is interrupted,that mean the dead load-balance is alive now.");
                        }
                        for (Map.Entry<LoadBalance<?>, Runnable> entry : shutdownTaskMap.entrySet()) {
                            LoadBalance<?> loadBalance = entry.getKey();
                            long lastZeroTime = loadBalance.lastZeroTime;
                            if (lastZeroTime == 0) continue;
                            long waitingTime = now() - lastZeroTime;
                            if (waitingTime > Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME) {
                                //do hook
                                log.info("ready to recycle load-balance{} and execute shutdown hook", loadBalance);
                                entry.getValue().run();
                                loadBalance.lbElement = null;
                                loadBalance.eventList = null;
                                loadBalance.attribute = null;
                                shutdownTaskMap.remove(loadBalance);
                            } else {
                                // 时间不够，算出最短时间

                                sleepTime = Math.min(Context.INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME - waitingTime, sleepTime);
                                LockSupport.unpark(sentinelThread);
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
        return lbElement.size();
    }

    public void addElement(T element) {
        lbElement.add(element);
        eventList.forEach(loadBalanceConsumer -> loadBalanceConsumer.accept(this));
        if (sentinelThread == null) return;
        lastZeroTime = 0;
        sentinelThread.interrupt();
    }

    public void removeElement(T element) {
        if (lbElement.remove(element) && lbElement.size() == 0 && sentinelThread != null) {
            lastZeroTime = now();
            LockSupport.unpark(sentinelThread);
        }
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

    private long now() {
        return System.currentTimeMillis();
    }
}
