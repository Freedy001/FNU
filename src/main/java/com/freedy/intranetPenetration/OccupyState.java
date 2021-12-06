package com.freedy.intranetPenetration;

import com.freedy.Struct;
import com.freedy.intranetPenetration.remote.ChanelWarehouse;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存池中的channel状态 <br/>
 * 该类的设计参考AQS的实现
 *
 * @author Freedy
 * @date 2021/11/19 17:29
 */
@Slf4j
@ToString
public class OccupyState {
    //公用变量
    //上次释放锁的时间
    private static final Map<Integer, long[]> lastReleaseTimeMap = new ConcurrentHashMap<>();
    //每个服务对应一个任务队列,同一个的服务的多个管道公用一个任务队列
    private static final Map<Integer, Queue<ForwardTask>> wakeupMap = new ConcurrentHashMap<>();
    //每个服务对应一个缩容临界值的次数
    private static final Map<Integer, AtomicInteger> shrinkCountMap = new ConcurrentHashMap<>();
    //每个服务对应一个是否检查缩容
    private static final Map<Integer, Boolean> checkShrink = new ConcurrentHashMap<>();
    private static final Map<Integer, Struct.BoolWithStamp> checkExpandMap = new ConcurrentHashMap<>();

    public static void initTaskQueue(int serverPort) {
        lastReleaseTimeMap.put(serverPort, new long[]{System.currentTimeMillis()});
        wakeupMap.put(serverPort, new ConcurrentLinkedQueue<>());
        shrinkCountMap.put(serverPort, new AtomicInteger());
        checkExpandMap.put(serverPort, new Struct.BoolWithStamp());
    }

    public static void removeTaskQueue(int serverPort) {
        lastReleaseTimeMap.remove(serverPort);
        wakeupMap.remove(serverPort);
        shrinkCountMap.remove(serverPort);
        checkExpandMap.remove(serverPort);
    }

    public static void inspectChannelState() {
        ChanelWarehouse.PORT_CHANNEL_CACHE.forEach((k, v) -> {
            int busy = getBusyChannelCount(v);
            try {
                int channelSize = v.size();
                int taskQueueSize = wakeupMap.get(k).size();
                log.debug("[REMOTE-HEART-RECEIVE]: service channel status port:{} total channel size: {} busy channel size:{}  taskQueue size: {}", k, channelSize, busy, taskQueueSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static int getBusyChannelCount(LoadBalance<Channel> loadBalance) {
        int count = 0;
        for (Channel channel : loadBalance.getAllSafely()) {
            if (ChannelUtils.getOccupy(channel).occupied.get())
                count++;
        }
        return count;
    }


    //是否被占用
    private final AtomicBoolean occupied = new AtomicBoolean(false);
    //占用者的channel
    @Getter
    private Channel receiverChannel;
    //该对象所属的管道
    private final Channel intranetChannel;
    //服务端口
    @Getter
    private final int serverPort;
    /*
     * 以下四个对象 在相同的serverPort的channel中所引用的对象是相同的
     * 言外之意，同一个服务的所有管道公用以下三个对象
     */
    //任务队列
    @Getter
    private final Queue<ForwardTask> taskQueue;
    //达到管道缩容临界值的次数
    private final AtomicInteger shrinkCount;
    //检测缩容
    private final long[] lastReleaseTime;
    private final Struct.BoolWithStamp checkExpand;

    public OccupyState(Channel intranetChannel, int serverPort) {
        this.intranetChannel = intranetChannel;
        this.serverPort = serverPort;
        this.lastReleaseTime = lastReleaseTimeMap.get(serverPort);
        this.taskQueue = wakeupMap.get(serverPort);
        this.shrinkCount = shrinkCountMap.get(serverPort);
        this.checkExpand = checkExpandMap.get(serverPort);
    }

    /**
     * 当管道数达到最小值，就不需要检测是否需要缩容了
     */
    public void lockShrinkCheck() {
        checkShrink.put(serverPort, false);
    }

    /**
     * 每次扩容是把检测开关都打开
     */
    public void unlockShrinkCheck() {
        checkShrink.put(serverPort, true);
    }


    public void lockExpandCheck(long timeout, TimeUnit unit) {
        int lastStamp = checkExpand.set(true);
        //超时自动解锁
        intranetChannel.eventLoop().schedule(() -> {
            //引入stamp 解决ABA问题
            if (checkExpand.getStamp() == lastStamp) {
                //解锁
                checkExpand.set(false);
                log.warn("expand-check-lock timed out.log,unlock unlock succeeded!");
            }
        }, timeout, unit);
    }

    public void lockExpandCheck() {
        checkExpand.set(true);
    }

    public void unlockExpandCheck() {
        checkExpand.set(false);
    }

    public boolean isOccupy() {
        return occupied.get();
    }


    public boolean tryOccupy(Channel channel) {
        if (occupied.compareAndSet(false, true)) {
            receiverChannel = channel;
            return true;
        }
        return receiverChannel == channel;
    }

    public void submitTask(ForwardTask task) {
        taskQueue.offer(task);

        if (checkExpand.get()) return;
        int taskQueueSize = taskQueue.size();
        if (taskQueueSize > 0) {
            /*
             * 在发送扩容命令后，会让channelCache短时间无法发送扩容命令。
             * 主要是防止重复提交扩容命令。指定解锁时间，防止通讯消息丢失，导致死锁。
             */
            lockExpandCheck(5, TimeUnit.SECONDS);
            //need to expand
            ChannelUtils.setCmd(intranetChannel, Protocol.EXPEND.param(taskQueueSize >> 1 + taskQueueSize));
        }
    }

    public void release(Channel channel) {
        if (channel != receiverChannel) {
            log.warn("An illegal release request are found,Because the current occupy channel[{}] is not equal to the giving one[{}]", receiverChannel, channel);
            return;
        }
        ForwardTask task = taskQueue.poll();
        if (task == null) {
            receiverChannel = null;
            occupied.set(false);
            if (Optional.ofNullable(checkShrink.get(serverPort)).orElse(true)) {
                shrinkTask();
            }
        } else {
            this.receiverChannel = task.receiverChannel();
            //执行任务
            task.execute(intranetChannel);
            //获取任务队列中所有能用receiverChannel发给用户的任务并执行
            executeSameReceiverTask(task);
        }
        lastReleaseTime[0] = System.currentTimeMillis();
    }

    private void shrinkTask() {
        LoadBalance<Channel> loadBalance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(serverPort);
        int busyCount = getBusyChannelCount(loadBalance);
        //预留1/4的管道缓存空间
        if (loadBalance.size() > busyCount + (busyCount >> 1)) {
            //shrink
            shrinkCount.incrementAndGet();
        } else {
            /*
             * 在高并发场景下，busyCount会变化的非常快，如果仅当条件触发一次(remainCount < totalCount)
             * 就行缩容的化，会导致系统性能低下。所以需加上一个时间限制与次数限制，确保当前场景用户的请求次数不频繁。
             */
            shrinkCount.set(0);
        }
        if (shrinkCount.get() >= 5) {
            lockShrinkCheck();
            intranetChannel.eventLoop().schedule(() -> {
                int bCount = getBusyChannelCount(loadBalance);
                //预留1/4的管道缓存空间
                int shrink = loadBalance.size() - (bCount + (bCount >> 1));
                if (shrink < 0) {
                    log.info("ready to send shrink command to client,shrink count:{}", shrink);
                    ChannelUtils.setCmd(intranetChannel, Protocol.SHRINK.param(shrink));
                }
                shrinkCount.set(0);
                unlockShrinkCheck();
            }, 30, TimeUnit.SECONDS);
        }
    }


    /**
     * 执行任务队列中所有能与task共用receiverChannel的任务
     *
     * @param task 用于找到与之receiverChannel相同的task
     * @return 执行次数
     */
    public int executeSameReceiverTask(ForwardTask task) {
        int counter = 0;
        Iterator<ForwardTask> iterator = taskQueue.iterator();
        while (iterator.hasNext()) {
            ForwardTask item = iterator.next();
            if (item.receiverChannel() == task.receiverChannel()) {
                intranetChannel.eventLoop().execute(() -> item.execute(intranetChannel));
                counter++;
                iterator.remove();
            }
        }
        return counter;
    }
}
