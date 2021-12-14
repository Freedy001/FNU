package com.freedy.intranetPenetration.remote;

import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


/**
 * @author Freedy
 * @date 2021/11/17 17:03
 */
@Slf4j
@Part(type = BeanType.PROTOTYPE)
public class ChanelWarehouse extends SimpleChannelInboundHandler<Struct.ConfigGroup> {
    //对外提供服务的 父管道
    @Inject("remoteServerForBrowserParentChannel")
    private Map<Integer,Channel> remoteServerForBrowserParentChannel;
    //管道缓存
    @Inject("portChannelCache")
    private Map<Integer, LoadBalance<Channel>> portChannelCache;
    @Inject
    private BeanFactory factory;
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
                    portChannelCache.remove(serverPort);
                    OccupyState.removeTaskQueue(serverPort);
                    remoteServerForBrowserParentChannel.get(serverPort).close().addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("shutdown success! server on port: {}", serverPort);
                        }
                    });
                });
                portChannelCache.put(serverPort, loadBalance);
                OccupyState.initTaskQueue(serverPort);
            }
        }
        loadBalance.addElement(channel);
        ChannelUtils.setOccupy(channel, new OccupyState(channel, serverPort));
        ChannelUtils.setGroup(channel, group);

        channel.writeAndFlush(Protocol.ACK);
        log.debug("[server] send ACK");
    }

}
