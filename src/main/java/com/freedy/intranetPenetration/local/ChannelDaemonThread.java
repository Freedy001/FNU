package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Freedy
 * @date 2021/11/18 20:04
 */
public class ChannelDaemonThread extends TimerTask {

    @Override
    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public void run() {
        Map<Struct.SocketQuad, List<Channel>> remoteChannelList = IntranetClient.remoteChannelMap;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        do {
            remoteChannelList.forEach((group, channelList) -> {
                int size = channelList.size();
                if (size < Context.INTRANET_CHANNEL_CACHE_SIZE) {
                    executor.submit(() -> {
                        for (int i = size; i < Context.INTRANET_CHANNEL_CACHE_SIZE; i++) {
                            //需要重新建立管道连接
                            IntranetClient.initConnection(group);
                        }
                    });
                }
                //发送心跳包
                channelList.forEach(channel -> ChannelUtils.sendString(channel, Protocol.HEARTBEAT_LOCAL_NORMAL_MSG));
            });

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
    }


}
