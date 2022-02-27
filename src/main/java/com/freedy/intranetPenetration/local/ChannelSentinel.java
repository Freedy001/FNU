package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
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
@Part
@Slf4j
public class ChannelSentinel extends TimerTask {

    private final Map<Struct.ConfigGroup, Integer> groupBadConnectTimes = new HashMap<>();
    @Inject
    private ClientConnector clientConnector;

    @Inject("remoteChannelMap")
    private Map<Struct.ConfigGroup, Set<Channel>> remoteChannelMap;

    @Inject
    private LocalProp prop;

    private ExecutorService executor;

    @PostConstruct
    private void initExecutor(){
        if (prop != null && prop.getEnabled())
            executor = Executors.newFixedThreadPool(prop.getConfigGroupList().size());
    }

    @Override
    @SuppressWarnings("all")
    public void run() {
        log.info("start channel heartbeat and size check sentinel thread");
        do {
            try {
                remoteChannelMap.forEach((group, channelList) -> {
                    doCheck(group, channelList);

                    doHeartbeat(channelList);
                });
                Thread.sleep(3_000);
            } catch (InterruptedException ignored) {
            }
        } while (true);
    }

    public void doCheck(Struct.ConfigGroup group, Set<Channel> channelList) {
        executor.submit(() -> {
            int size = channelList.size();
            Integer bConn = Optional.ofNullable(groupBadConnectTimes.get(group)).orElse(0);
            if (bConn >= Context.INTRANET_MAX_BAD_CONNECT_TIMES) {
                log.warn("bad-connection[{}] has exceed the max bad-connect times[{}]", bConn, Context.INTRANET_MAX_BAD_CONNECT_TIMES);
                if (size == 0) {
                    //只建立一次试探性连接
                    if (!clientConnector.initConnection(group)) {
                        groupBadConnectTimes.merge(group, 1, Integer::sum);
                        return;  //连接失败 do nothing
                    }
                    //连接成功，继续开通其他连接
                }
                groupBadConnectTimes.remove(group);
            }

            int expect = prop.getMinChannelCount();
            if (size < expect) {
                log.warn("connection[{}] less than expectation[{}],ready to extend connection", size, expect);
                for (int i = size; i < expect; i++) {
                    //需要重新建立管道连接
                    if (!clientConnector.initConnection(group)) {
                        groupBadConnectTimes.merge(group, 1, Integer::sum);
                    }
                }
            }
        });
    }


    private void doHeartbeat(Set<Channel> channelList) {
        //定时发送心跳包
        for (Channel channel : channelList) {
            if (ChannelUtils.isInit(channel)) {
                ChannelUtils.setCmdAndSendIfAbsent(channel, Protocol.HEARTBEAT_LOCAL_NORMAL_MSG);
            } else {
                int failTimes = Optional.ofNullable(ChannelUtils.getFileTimes(channel)).orElse(0);
                if (failTimes >= 2) {
                    //6-9秒 无初始化表示握手失败 需要撤掉该channel
                    channelList.remove(channel);
                    continue;
                }
                ChannelUtils.setFailTimes(channel, failTimes + 1);
            }
        }
    }

}
