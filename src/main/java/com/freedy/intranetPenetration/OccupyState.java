package com.freedy.intranetPenetration;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存池中的channel状态
 * @author Freedy
 * @date 2021/11/19 17:29
 */
public class OccupyState {
    //是否被占用
    AtomicBoolean occupied = new AtomicBoolean(false);
    //占用者的channel
    Channel receiverChannel;
    //需要被唤醒的队列
    Queue<Promise<Channel>> wakeupList = new ConcurrentLinkedQueue<>();

    public boolean tryOccupy(Channel channel) {
        if (occupied.compareAndSet(false, true)){
            receiverChannel=channel;
            return true;
        }
        return receiverChannel==channel;
    }

    public void submitTask(Promise<Channel> promise){
        wakeupList.offer(promise);
    }

    public void release(Channel channel){
        if (channel!=receiverChannel) throw new IllegalArgumentException("please call tryOccupy first");
        Promise<Channel> promise = wakeupList.poll();

        receiverChannel=null;

    }
}
