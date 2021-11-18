package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/11/17 17:03
 */
public class ChanelWarehouse extends SimpleChannelInboundHandler<Struct.SocketQuad> {
    //管道缓存
    public final static Map<Integer,LoadBalance<Channel>> PORT_CHANNEL_CACHE=new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Struct.SocketQuad group) throws Exception {
        Channel channel = ctx.channel();
        int remotePort = group.remotePort();
        LoadBalance<Channel> loadBalance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(remotePort);
        if (loadBalance == null) {
            loadBalance = LoadBalanceFactory.produce(Context.PORT_CHANNEL_CACHE_LB_NAME);
            ChanelWarehouse.PORT_CHANNEL_CACHE.put(remotePort, loadBalance);
        }
        loadBalance.addElement(channel);

        channel.writeAndFlush(Protocol.ACK);
    }

}
