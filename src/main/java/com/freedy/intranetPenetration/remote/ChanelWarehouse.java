package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Freedy
 * @date 2021/11/17 17:03
 */
@Slf4j
public class ChanelWarehouse extends SimpleChannelInboundHandler<Struct.ConfigGroup> {
    //管道缓存
    public final static Map<Integer, LoadBalance<Channel>> PORT_CHANNEL_CACHE = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Struct.ConfigGroup group) {
        log.debug("[server]接收配置消息[{}]", group);
        Channel channel = ctx.channel();
        //初始化 管道池
        int serverPort = group.getRemoteServerPort();
        LoadBalance<Channel> loadBalance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(serverPort);
        createLB:
        if (loadBalance == null) {
            synchronized (PORT_CHANNEL_CACHE) {
                if ((loadBalance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(serverPort)) != null) break createLB;
                assert Context.PORT_CHANNEL_CACHE_LB_NAME != null;
                loadBalance = LoadBalanceFactory.produce(Context.PORT_CHANNEL_CACHE_LB_NAME);
                ChanelWarehouse.PORT_CHANNEL_CACHE.put(serverPort, loadBalance);
            }
        }
        loadBalance.addElement(channel);
        ChannelUtils.setOccupy(channel, new OccupyState(channel, group.getRemoteServerPort()));
        ChannelUtils.setGroup(channel, group);

        channel.writeAndFlush(Protocol.ACK);
        log.debug("[server]发送ACK");
    }

}
