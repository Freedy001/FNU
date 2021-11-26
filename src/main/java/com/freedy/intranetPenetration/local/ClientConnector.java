package com.freedy.intranetPenetration.local;

import com.freedy.AuthenticAndDecrypt;
import com.freedy.AuthenticAndEncrypt;
import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.ChannelSentinel;
import com.freedy.intranetPenetration.ParentChannelFuture;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.remote.IntranetServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Freedy
 * @date 2021/11/17 14:16
 */
@Slf4j
public class ClientConnector {

    private final static Bootstrap bootstrap = new Bootstrap();
    public final static Map<Struct.ConfigGroup, List<Channel>> remoteChannelMap = new ConcurrentHashMap<>();
    public static Thread sentinelDaemonThread;

    public static void main(String[] args) {
        ClientConnector.start();
        LockSupport.park();
    }

    public static ParentChannelFuture start() {
        final ParentChannelFuture future = new ParentChannelFuture();
        bootstrap.group(new NioEventLoopGroup(0, new DefaultThreadFactory("intranetClient")))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        future.setChannel(ch);
                    }
                });


        //初始化配置消息
        for (Struct.ConfigGroup group : Context.INTRANET_GROUPS) {
            remoteChannelMap.put(group, new CopyOnWriteArrayList<>());
        }

        //开始连接客户端
        sentinelDaemonThread = new Thread(new ChannelSentinel(), "Channel Sentinel Daemon Thread");
        sentinelDaemonThread.setDaemon(true);
        sentinelDaemonThread.start();


        log.info("Intranet client started success!");
        return future;
    }

    /**
     * 连接到远程服务
     * @param group 配置消息
     * @return 是否连接成功
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean initConnection(Struct.ConfigGroup group) {
        try {
            Channel channel = bootstrap.connect(group.getRemoteAddress(), group.getRemotePort()).sync().channel();
            channel.pipeline().addLast(
                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new AuthenticAndEncrypt(),
                    new AuthenticAndDecrypt(Protocol::invokeHandler),
                    new ObjectEncoder(),
                    new ObjectDecoder(ClassResolvers.cacheDisabled(IntranetServer.class.getClassLoader())),
                    new ClientHandshake(group)
            );

            log.debug("[client]发送配置消息[{}]", group);
            channel.writeAndFlush(group);

            List<Channel> list = remoteChannelMap.get(group);
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                remoteChannelMap.put(group, list);
            }
            list.add(channel);
        } catch (Exception e) {
            log.error("[EXCEPTION]: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 连接到本地服务
     * @param group 配置消息
     * @param remoteChannel 远程通讯的管道
     * @return 连接到本地服务后的管道
     */
    public static Channel localServerConnect(Struct.ConfigGroup group,Channel remoteChannel) {
        try {
            Channel channel = bootstrap.connect(group.getLocalServerAddress(), group.getLocalServerPort()).sync().channel();
            channel.pipeline().addLast(
                    new ResponseForward(remoteChannel)
            );
            return channel;
        } catch (Exception e) {
            log.error("[EXCEPTION]: {}", e.getMessage());
        }
        return null;
    }


}
