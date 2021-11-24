package com.freedy.intranetPenetration;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 一个线程,用于维护客户端与服务端的管道连接数量与心跳包
 *
 * @author Freedy
 * @date 2021/11/18 20:04
 */
@Slf4j
public class ChannelSentinel extends TimerTask {

    private final Map<Struct.ConfigGroup, Integer> groupBadConnectTimes = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Context.INTRANET_GROUPS.length);

    @Override
    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public void run() {
        do {
            try {
                ClientConnector.remoteChannelMap.forEach((group, channelList) -> {
                    doCheck(group, channelList);

                    doHeartbeat(channelList);
                });
                Thread.sleep(3_000);
            } catch (InterruptedException ignored) {
            }
        } while (true);
    }

    public void doCheck(Struct.ConfigGroup group, List<Channel> channelList) {
        executor.submit(() -> {
            int size = channelList.size();
            Integer bConn = Optional.ofNullable(groupBadConnectTimes.get(group)).orElse(0);
            if (bConn >= Context.INTRANET_MAX_BAD_CONNECT_TIMES) {
                log.warn("bad-connection[{}] has exceed the max bad-connect times[{}]", bConn, Context.INTRANET_MAX_BAD_CONNECT_TIMES);
                if (size == 0) {
                    //只建立一次试探性连接
                    if (!ClientConnector.initConnection(group)) {
                        groupBadConnectTimes.merge(group, 1, Integer::sum);
                        return;  //连接失败 do nothing
                    }
                    //连接成功，继续开通其他连接
                }
                groupBadConnectTimes.remove(group);
            }

            int expect = Context.INTRANET_CHANNEL_CACHE_MIN_SIZE;
            if (size < expect) {
                log.warn("connection[{}] less than expectation[{}],ready to extend connection", size, expect);
                for (int i = size; i < expect; i++) {
                    //需要重新建立管道连接
                    if (!ClientConnector.initConnection(group)) {
                        groupBadConnectTimes.merge(group, 1, Integer::sum);
                    }
                }
            }
        });
    }


    private void doHeartbeat(List<Channel> channelList) {
        //定时发送心跳包
        for (Channel channel : channelList) {
            if (ChannelUtils.isInit(channel)) {
                ChannelUtils.setCmdAndSendIfAbsent(channel, Protocol.HEARTBEAT_LOCAL_NORMAL_MSG);
            }
        }
    }

}
