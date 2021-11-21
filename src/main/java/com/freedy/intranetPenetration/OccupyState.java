package com.freedy.intranetPenetration;

import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.intranetPenetration.remote.ChanelWarehouse;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存池中的channel状态 <br/>
 * 该类的设计参考AQS的实现
 *
 * @author Freedy
 * @date 2021/11/19 17:29
 */
@Slf4j
@ToString
@RequiredArgsConstructor
public class OccupyState {
    //是否被占用
    private final AtomicBoolean occupied = new AtomicBoolean(false);
    //占用者的channel
    @Getter
    private Channel receiverChannel;
    //该对象所属的管道
    private final Channel intranetChannel;
    @Getter
    private final Struct.ConfigGroup group;
    //公用变量
    //需要被唤醒的队列
    private static final Queue<ForwardTask> wakeupList = new ConcurrentLinkedQueue<>();


    public static void inspectChannelState() {
        ChanelWarehouse.PORT_CHANNEL_CACHE.forEach((k, v) -> {
            int busy = 0;
            for (Channel channel : v.getAllSafely()) {
                if (ChannelUtils.getOccupy(channel).occupied.get()) {
                    busy++;
                }
            }
            try {
                int channelSize = v.size();
                int taskQueueSize = wakeupList.size();
                log.debug("[REMOTE-HEART-RECEIVE]: service channel status port:{} total channel size: {} busy channel size:{}  taskQueue size: {}", k, channelSize, busy, taskQueueSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean tryOccupy(Channel channel) {
        if (occupied.compareAndSet(false, true)) {
            receiverChannel = channel;
            return true;
        }
        return receiverChannel == channel;
    }

    public void submitTask(ForwardTask task) {
        wakeupList.offer(task);
        int totalChannelSize = ChanelWarehouse.PORT_CHANNEL_CACHE
                .get(group.getRemoteServerPort())
                .size();
        if (wakeupList.size() > totalChannelSize)
        {
            //need to expand
            ChannelUtils.sendString(intranetChannel, Protocol.EXPEND+"");
        }
    }

    public void release(Channel channel) {
        if (channel != receiverChannel) {
            log.warn("An illegal release request are found,Because the current occupy channel[{}] is not equal to the giving one[{}]", receiverChannel, channel);
            return;
        }
        ForwardTask task = wakeupList.poll();
        if (task == null) {
            receiverChannel = null;
            occupied.set(false);
            return;
        }
        this.receiverChannel = task.receiverChannel();
        //执行任务
        task.execute(intranetChannel);
        //获取任务队列中所有能用receiverChannel发给用户的任务并执行
        Iterator<ForwardTask> iterator = wakeupList.iterator();
        while (iterator.hasNext()) {
            ForwardTask item = iterator.next();
            if (item.receiverChannel() == task.receiverChannel()) {
                item.execute(intranetChannel);
                iterator.remove();
            }
        }
    }
}
