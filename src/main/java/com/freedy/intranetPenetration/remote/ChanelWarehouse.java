package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Freedy
 * @date 2021/11/17 17:03
 */
public class ChanelWarehouse extends SimpleChannelInboundHandler<Struct.ConfigGroup> {
    //管道缓存
    public final static Map<Integer, LoadBalance<Channel>> PORT_CHANNEL_CACHE = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Struct.ConfigGroup group) throws Exception {
        Channel channel = ctx.channel();
        int serverPort = group.remoteServerPort();
        LoadBalance<Channel> loadBalance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(serverPort);
        if (loadBalance == null) {
            loadBalance = LoadBalanceFactory.produce(Context.PORT_CHANNEL_CACHE_LB_NAME);
            ChanelWarehouse.PORT_CHANNEL_CACHE.put(serverPort, loadBalance);
        }
        loadBalance.addElement(channel);
        ChannelUtils.setOccupy(channel,new OccupyState());
        channel.writeAndFlush(Protocol.ACK);
    }

}
