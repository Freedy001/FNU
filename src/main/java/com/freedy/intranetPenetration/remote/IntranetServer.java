package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.nio.charset.Charset;

/**
 * @author Freedy
 * @date 2021/11/17 15:23
 */
public class IntranetServer {
    public static final NioEventLoopGroup workGroup = new NioEventLoopGroup(0,new DefaultThreadFactory("intranet-server-worker"));


    public static Channel start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        Channel channel = bootstrap.group(
                        new NioEventLoopGroup(1),
                        workGroup
                )
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(IntranetServer.class.getClassLoader())),
                                new ChanelWarehouse(),
                                new ServerHandshake()
                        );
                    }
                })
                .bind(Context.INTRANET_REMOTE_PORT).sync().channel();

        System.out.println("Intranet-Master-Server started success on port:" + Context.INTRANET_REMOTE_PORT);
        return channel;

    }

}
