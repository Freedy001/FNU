package com.freedy;

import com.freedy.intranetPenetration.ChannelSentinel;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.local.LocalProp;
import com.freedy.intranetPenetration.remote.ChanelWarehouse;
import com.freedy.intranetPenetration.remote.RemoteProp;
import com.freedy.intranetPenetration.remote.ServerHandshake;
import com.freedy.jumpProxy.HttpProxyProp;
import com.freedy.jumpProxy.ReverseProxyProp;
import com.freedy.jumpProxy.local.MsgForward;
import com.freedy.jumpProxy.remote.HttpProxyHandler;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Freedy
 * @date 2021/11/6 16:19
 */
@Slf4j
@Part(configure = true)
public class ServerStarter {

    @Getter
    private long reverseStartTime = 0;
    @Getter
    private long httpStartTime = 0;
    @Getter
    private long intranetLocalStartTime = 0;
    @Getter
    private long intranetRemoteStartTime = 0;
    @Inject
    private BeanFactory factory;

    @Bean
    public NioEventLoopGroup work(){
        return new NioEventLoopGroup(0);
    }

    @Bean
    public Channel reverseProxy(ReverseProxyProp reverseProxyProp,@Inject("worker") NioEventLoopGroup worker) throws Exception {
        if (!reverseProxyProp.isEnabled()) return null;
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(factory.getBean(MsgForward.class));
                    }
                });

        Channel channel = bootstrap.bind(reverseProxyProp.getPort()).sync().channel();
        log.info("reverseProxy service started success on http://127.0.0.1:{}/", reverseProxyProp.getPort());
        reverseStartTime = System.currentTimeMillis();
        return channel;
    }

    @Bean
    public Channel httpProxy(HttpProxyProp httpProxyProp,@Inject("worker") NioEventLoopGroup worker) throws Exception {
        if (!httpProxyProp.isEnabled()) return null;
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if (!httpProxyProp.isJumpEndPoint()) {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    factory.getBean(HttpProxyHandler.class)
                            );
                        } else {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                    new LengthFieldPrepender(4),
                                    new AuthenticAndEncrypt(),
                                    new AuthenticAndDecrypt(null),
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    factory.getBean(HttpProxyHandler.class)
                            );
                        }
                    }
                });
        Channel channel = bootstrap.bind(httpProxyProp.getPort()).sync().channel();
        log.info("httpProxy server start on http://127.0.0.1:{}/", httpProxyProp.getPort());
        httpStartTime = System.currentTimeMillis();
        return channel;
    }


    @Bean
    public byte[] pac(ReverseProxyProp reverseProxyProp) {
        if (reverseProxyProp.isJumpEndPoint()) {
            InputStream pacFile = MsgForward.class.getClassLoader().getResourceAsStream("pac");
            try {
                assert pacFile != null;
                log.info("pac server start success on url:http://127.0.0.1:{}/pac", reverseProxyProp.getPort());
                return pacFile.readAllBytes();
            } catch (Exception e) {
                log.error("pac server start failed!because {}", e.getMessage());
            }
        }
        return null;
    }

    @Bean
    public Bootstrap bootstrap(NioEventLoopGroup worker) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                    }
                });
        return bootstrap;
    }

    /**
     * 内网穿透本地服务 配置信息与之对应的缓存管道map
     */
    @Bean("remoteChannelMap")
    public Map<Struct.ConfigGroup, List<Channel>> intranetLocalServer() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 启动内网穿透本地服务
     */
    @Bean("sentinelDaemonThread")
    public Thread intranetLocalServer(
            @Inject("remoteChannelMap") Map<Struct.ConfigGroup, List<Channel>> remoteChannelMap,
            LocalProp localProp, ChannelSentinel sentinel) {
        if (!localProp.isEnabled()) return null;
        //初始化配置消息
        for (Struct.ConfigGroup group : localProp.getConfigGroupList()) {
            remoteChannelMap.put(group, new CopyOnWriteArrayList<>());
        }
        //开始连接客户端
        Thread sentinelDaemonThread = new Thread(sentinel, "Channel Sentinel Daemon Thread");
        sentinelDaemonThread.setDaemon(true);
        sentinelDaemonThread.start();
        log.info("Intranet client started success!");
        intranetLocalStartTime = System.currentTimeMillis();
        return sentinelDaemonThread;
    }

    /**
     * 内网穿透远程服务 对外服务的端口号 与 与本地已经建立长连接的缓存管道map
     */
    @Bean("portChannelCache")
    public Map<Integer, LoadBalance<Channel>> intranetRemoteServer() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 内网穿透远程服务 对外服务的端口号 与 对外服务的父管道
     */
    @Bean
    public Map<Integer,Channel> remoteServerForBrowserParentChannel() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 启动内网穿透远程服务
     */
    @Bean
    public Channel intranetRemoteServer(RemoteProp prop,NioEventLoopGroup worker) throws InterruptedException {
        if (!prop.isEnabled()) return null;
        ServerBootstrap bootstrap = new ServerBootstrap();
        Channel channel = bootstrap.group(
                        new NioEventLoopGroup(1),
                        worker
                )
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                new LengthFieldPrepender(4),
                                new AuthenticAndEncrypt(),
                                new AuthenticAndDecrypt(Protocol::invokeHandler),
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(ServerStarter.class.getClassLoader())),
                                factory.getBean(ChanelWarehouse.class),
                                factory.getBean(ServerHandshake.class)
                        );
                    }
                })
                .bind(prop.getPort()).sync().channel();

        log.info("Intranet-Remote-Master-Server started success on http://127.0.0.1:{}/",prop.getPort());
        intranetRemoteStartTime=System.currentTimeMillis();
        return channel;
    }

}
