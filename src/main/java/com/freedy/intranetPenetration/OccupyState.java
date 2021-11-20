package com.freedy.intranetPenetration;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.ToString;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存池中的channel状态
 *
 * @author Freedy
 * @date 2021/11/19 17:29
 */
@ToString
public class OccupyState {
    //是否被占用
    private final AtomicBoolean occupied = new AtomicBoolean(false);
    //占用者的channel
    @Getter
    private Channel receiverChannel;
    private int reentrantCount = 0;
    //需要被唤醒的队列
    private final Queue<ForwardTask> wakeupList = new ConcurrentLinkedQueue<>();

    public boolean tryOccupy(Channel channel) {
        if (occupied.compareAndSet(false, true)) {
            receiverChannel = channel;
            return true;
        }
        if (receiverChannel == channel) {
            reentrantCount++;
            return true;
        }
        return false;
    }

    public void submitTask(ForwardTask task) {
        wakeupList.offer(task);
    }

    public void release(Channel channel) {
        if (channel != receiverChannel) throw new IllegalArgumentException("please call tryOccupy first");
        if (reentrantCount != 0) {
            reentrantCount--;
            return;
        }
        ForwardTask task = wakeupList.poll();
        if (task != null) {
            this.receiverChannel = task.getReceiverChannel();
            channel.eventLoop().execute(task);
            return;
        }
        receiverChannel = null;
        occupied.set(false);
    }
}
