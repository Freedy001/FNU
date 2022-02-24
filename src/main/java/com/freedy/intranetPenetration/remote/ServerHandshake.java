package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import com.freedy.utils.ChannelUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Freedy
 * @date 2021/11/17 17:57
 */
@Slf4j
@ChannelHandler.Sharable
@Part(type = BeanType.SINGLETON)
public class ServerHandshake extends SimpleChannelInboundHandler<String> {

    @Inject("remoteServerForBrowserParentChannel")
    private Map<Integer,Channel> remoteServerForBrowserParentChannel;
    @Inject
    private NioEventLoopGroup worker;
    @Inject
    private BeanFactory factory;
    @Inject("portChannelCache")
    private Map<Integer, LoadBalance<Channel>> portChannelCache;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith(Protocol.CONNECTION_SUCCESS_MSG)) {
            log.debug("[server]接收CONNECT ESTABLISH SUCCEED!");

            Channel channel = ctx.channel();
            if (channel.isActive()) {
                channel.writeAndFlush(Protocol.ACK).addListener(future -> {
                    if (future.isSuccess()) {
                        log.debug("[server]发送ACK");
                        //移除握手处理器，创建新的处理器
                        channel.pipeline().remove(ObjectEncoder.class);
                        channel.pipeline().remove(ObjectDecoder.class);
                        channel.pipeline().remove(ChanelWarehouse.class);
                        channel.pipeline().remove(ServerHandshake.class);
                        channel.pipeline().addLast(
                                factory.getBean(HeartBeatHandler.class),
                                factory.getBean(ResponseListener.class)
                        );
                        channel.pipeline().addFirst(
                                new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS)
                        );
                    }
                });

            }
            Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
            final int remoteServerPort = group.getRemoteServerPort();
            if (!remoteServerForBrowserParentChannel.containsKey(remoteServerPort)) {
                //noinspection SynchronizeOnNonFinalField
                synchronized (remoteServerForBrowserParentChannel){
                    if (remoteServerForBrowserParentChannel.containsKey(remoteServerPort)) return;
                    //启动服务
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    NioEventLoopGroup bossEvent = new NioEventLoopGroup(1);
                    Channel parentChannel = bootstrap.group(bossEvent, worker)
                            .channel(NioServerSocketChannel.class)
                            .option(ChannelOption.SO_BACKLOG, 10240)
                            .childHandler(new ChannelInitializer<>() {
                                @Override
                                protected void initChannel(Channel channel) {
                                    RequestReceiver requestReceiver = factory.getBean(RequestReceiver.class);
                                    requestReceiver.setLb(portChannelCache.get(remoteServerPort));
                                    channel.pipeline().addLast(
                                            requestReceiver
                                    );
                                }
                            })
                            .bind(remoteServerPort).sync().channel();
                    remoteServerForBrowserParentChannel.put(remoteServerPort, parentChannel);
                    ChannelUtils.setBossEvent(parentChannel,bossEvent);
                    log.info("Intranet-Remote-Slave-Server started success on http://127.0.0.1:{}/", remoteServerPort);
                }
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[EXCEPTION]: " + cause.getMessage());
    }
}
