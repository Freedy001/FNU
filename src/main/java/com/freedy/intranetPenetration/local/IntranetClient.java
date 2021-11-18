package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.remote.IntranetServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Freedy
 * @date 2021/11/17 14:16
 */
public class IntranetClient {

    public final static Bootstrap bootstrap = new Bootstrap();
    public final static Map<Struct.SocketQuad, List<Channel>> remoteChannelMap = new ConcurrentHashMap<>();
    public static AtomicInteger badConnectionTimes=new AtomicInteger(0);

    public static void main(String[] args) {
        bootstrap.group(new NioEventLoopGroup(0))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true);

        for (Struct.SocketQuad group : Context.INTRANET_GROUPS) {
            for (int i = 0; i < Context.INTRANET_CHANNEL_CACHE_SIZE; i++) {
                initConnection(group);
            }
        }

        Thread thread = new Thread(new ChannelDaemonThread(), "Channel Daemon Thread");
        thread.start();
        thread.setDaemon(true);

        LockSupport.park();
    }


    public static void initConnection(Struct.SocketQuad group) {
        if (badConnectionTimes.get()>=Context.INTRANET_MAX_BAD_CONNECT_TIMES){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ChannelFuture channelFuture = bootstrap.connect(group.remoteAddress(), group.remotePort());
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                badConnectionTimes.incrementAndGet();
                return;
            }
            Channel channel = channelFuture.channel();
            channel.pipeline().addLast(
                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new ObjectEncoder(),
                    new ObjectDecoder(ClassResolvers.cacheDisabled(IntranetServer.class.getClassLoader())),
                    new ClientInitHandler(group)
            );
            channel.writeAndFlush(group);
            List<Channel> list = remoteChannelMap.get(group);
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                remoteChannelMap.put(group, list);
            }
            list.add(channel);
        });

    }

}
