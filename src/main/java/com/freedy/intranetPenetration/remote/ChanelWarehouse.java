package com.freedy.intranetPenetration.remote;

import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


/**
 * @author Freedy
 * @date 2021/11/17 17:03
 */
@Slf4j
@ChannelHandler.Sharable
@Part(type = BeanType.SINGLETON)
public class ChanelWarehouse extends SimpleChannelInboundHandler<Struct.ConfigGroup> {
    //对外提供服务的 父管道
    @Inject("remoteServerForBrowserParentChannel")
    private Map<Integer,Channel> remoteServerForBrowserParentChannel;
    //管道缓存 LoadBalance可以理解为一个List
    @Inject("portChannelCache")
    private Map<Integer, LoadBalance<Channel>> portChannelCache;

    @Inject
    private RemoteProp prop;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Struct.ConfigGroup group) {
        log.debug("[server]receive config message[{}]", group);
        Channel channel = ctx.channel();
        //初始化 管道池
        int serverPort = group.getRemoteServerPort();
        LoadBalance<Channel> loadBalance = portChannelCache.get(serverPort);
        createLB:
        if (loadBalance == null) {
            synchronized (ChanelWarehouse.class) {
                if ((loadBalance = portChannelCache.get(serverPort)) != null) break createLB;
                loadBalance = LoadBalanceFactory.produce(prop.getLoadBalancing());
                loadBalance.registerShutdownHook(() -> {
                    OccupyState.removeTaskQueue(serverPort);
                    Channel parentChannel = remoteServerForBrowserParentChannel.get(serverPort);
                    parentChannel.close().addListener(future -> {
                        if (future.isSuccess()) {
                            NioEventLoopGroup bossEvent = ChannelUtils.getBossEvent(parentChannel);
                            if (bossEvent != null) {
                                bossEvent.shutdownGracefully();
                            }
                            log.info("shutdown success! server on port: {}", serverPort);
                        }
                    });
                    remoteServerForBrowserParentChannel.remove(serverPort);
                    portChannelCache.remove(serverPort);
                });
                OccupyState.initTaskQueue(serverPort);
                //必须为最后一步执行
                portChannelCache.put(serverPort, loadBalance);
            }
        }
        loadBalance.addElement(channel);
        ChannelUtils.setOccupy(channel, new OccupyState(channel, serverPort));
        ChannelUtils.setGroup(channel, group);

        channel.writeAndFlush(Protocol.ACK);
        log.debug("[server] send ACK");
    }

}
