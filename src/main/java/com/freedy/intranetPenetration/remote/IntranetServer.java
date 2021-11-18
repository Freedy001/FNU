package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.ArrayList;

/**
 * @author Freedy
 * @date 2021/11/17 15:23
 */
public class IntranetServer {

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Channel> list = new ArrayList<>();

        {
            ServerBootstrap bootstrap = new ServerBootstrap();
            Channel channel = bootstrap.group(new NioEventLoopGroup(1), new NioEventLoopGroup(0))
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
                                    new ServerInitHandler()
                            );
                        }
                    })
                    .bind(Context.INTRANET_REMOTE_PORT).sync().channel();
            list.add(channel);
            System.out.println("Intranet-Master-Server started success on port:" + Context.INTRANET_REMOTE_PORT);
        }

        for (Struct.SocketQuad group : Context.INTRANET_GROUPS) {
            int remotePort = group.remotePort();
            ServerBootstrap bootstrap = new ServerBootstrap();
            Channel channel = bootstrap.group(new NioEventLoopGroup(1), new NioEventLoopGroup(0))
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 10240)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                 new RequestReceiver(remotePort)
                            );
                        }
                    })
                    .bind(remotePort).sync().channel();
            list.add(channel);
            System.out.println("Intranet-Slave-Server started success on port:" + remotePort);
        }
        for (Channel channel : list) {
            channel.closeFuture().sync();
        }
    }

}
