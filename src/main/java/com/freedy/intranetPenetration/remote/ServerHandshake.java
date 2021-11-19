package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Freedy
 * @date 2021/11/17 17:57
 */
public class ServerHandshake extends SimpleChannelInboundHandler<String> {

    public final static Map<Integer,Channel> PORT_STARTED=new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        if (msg.startsWith(Protocol.CONNECTION_SUCCESS_MSG)) {
            //移除握手处理器，创建新的处理器
            Channel channel = channelHandlerContext.channel();
            channel.pipeline().remove(ObjectEncoder.class);
            channel.pipeline().remove(ObjectDecoder.class);
            channel.pipeline().remove(ChanelWarehouse.class);
            channel.pipeline().remove(ServerHandshake.class);


            Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
            final int remoteServerPort = group.remoteServerPort();
            if (!PORT_STARTED.containsKey(remoteServerPort)) {
                //启动服务
                ServerBootstrap bootstrap = new ServerBootstrap();
                Channel parentChannel = bootstrap.group(new NioEventLoopGroup(1),
                                IntranetServer.workGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 10240)
                        .childHandler(new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel channel) throws Exception {
                                channel.pipeline().addLast(
                                        new RequestReceiver(remoteServerPort)
                                );
                            }
                        })
                        .bind(remoteServerPort).sync().channel();

                System.out.println("Intranet-Slave-Server started success on port:" + remoteServerPort);
                PORT_STARTED.put(remoteServerPort,parentChannel);
            }
            ChannelUtils.sendString(channel,Protocol.ACK);

            channel.pipeline().addLast(
                    new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS),
                    new HeartBeatHandler(),
                    new ResponseListener()
            );
        }
    }
}
