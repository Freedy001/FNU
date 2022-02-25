package com.freedy;

import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.instruction.*;
import com.freedy.intranetPenetration.local.ChannelSentinel;
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
    private BeanFactory beanFactory;
    @Inject
    private NioEventLoopGroup worker;

    @Bean
    public NioEventLoopGroup work(){
        return new NioEventLoopGroup(0);
    }

    @Bean(conditionalOnBeanByType = ReverseProxyProp.class)
    public Channel reverseProxy(ReverseProxyProp reverseProxyProp) throws Exception {
        if (!reverseProxyProp.getEnabled()) return null;
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(beanFactory.getBean(MsgForward.class));
                    }
                });

        Channel channel = bootstrap.bind(reverseProxyProp.getPort()).sync().channel();
        log.info("reverseProxy service started success on http://127.0.0.1:{}/", reverseProxyProp.getPort());
        reverseStartTime = System.currentTimeMillis();
        return channel;
    }

    @Bean(conditionalOnBeanByType = HttpProxyProp.class)
    public Channel httpProxy(HttpProxyProp httpProxyProp,EncryptProp encryptProp) throws Exception {
        if (!httpProxyProp.getEnabled()) return null;
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if (!httpProxyProp.getJumpEndPoint()) {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    beanFactory.getBean(HttpProxyHandler.class)
                            );
                        } else {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                    new LengthFieldPrepender(4),
                                    new AuthenticAndEncrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken()),
                                    new AuthenticAndDecrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken(),null),
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    beanFactory.getBean(HttpProxyHandler.class)
                            );
                        }
                    }
                });
        Channel channel = bootstrap.bind(httpProxyProp.getPort()).sync().channel();
        log.info("httpProxy server start on http://127.0.0.1:{}/", httpProxyProp.getPort());
        httpStartTime = System.currentTimeMillis();
        return channel;
    }


    @Bean(conditionalOnBeanByType = ReverseProxyProp.class)
    public byte[] pac(ReverseProxyProp reverseProxyProp) {
        if (reverseProxyProp.getJumpEndPoint()) {
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
    public Bootstrap bootstrap() {
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
    @Bean(value = "sentinelDaemonThread",conditionalOnBeanByType = LocalProp.class)
    public Thread intranetLocalServer(
            @Inject("remoteChannelMap") Map<Struct.ConfigGroup, List<Channel>> remoteChannelMap,
                LocalProp localProp, ChannelSentinel sentinel) {
        if (!localProp.getEnabled()) return null;
        registerIntranetLocalInstructionHandler();
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
        Map<Integer, LoadBalance<Channel>> map = new ConcurrentHashMap<>();
        OccupyState.initPortChannelCache(map);
        return map;
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
    @Bean(conditionalOnBeanByType = RemoteProp.class)
    public Channel intranetRemoteServer(RemoteProp prop,EncryptProp encryptProp) throws InterruptedException {
        if (!prop.getEnabled()) return null;
        registerIntranetRemoteInstructionHandler();
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
                                new AuthenticAndEncrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken()),
                                new AuthenticAndDecrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken(),Protocol::invokeHandler),
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(ServerStarter.class.getClassLoader())),
                                beanFactory.getBean(ChanelWarehouse.class),
                                beanFactory.getBean(ServerHandshake.class)
                        );
                    }
                })
                .bind(prop.getPort()).sync().channel();

        log.info("Intranet-Remote-Master-Server started success on http://127.0.0.1:{}/",prop.getPort());
        intranetRemoteStartTime=System.currentTimeMillis();
        return channel;
    }

    private void registerIntranetRemoteInstructionHandler(){
        Protocol.HEARTBEAT_LOCAL_NORMAL_MSG.registerInstructionHandler(beanFactory.getBean(HeartBeatLocalNormalMsgHandler.class));
        Protocol.HEARTBEAT_LOCAL_ERROR_MSG.registerInstructionHandler(beanFactory.getBean(HeartbeatLocalErrorMsgHandler.class));
        Protocol.EXPEND_RESP.registerInstructionHandler(beanFactory.getBean(ExpendRespHandler.class));
        Protocol.REMOTE_SHUTDOWN.registerInstructionHandler(beanFactory.getBean(RemoteShutdownHandler.class));
        Protocol.SHRINK_RESP.registerInstructionHandler(beanFactory.getBean(ShrinkRespHandler.class));
    }

    private void registerIntranetLocalInstructionHandler(){
        Protocol.HEARTBEAT_REMOTE_NORMAL_MSG.registerInstructionHandler(new InstructionHandler() {
        });
        Protocol.HEARTBEAT_REMOTE_ERROR_MSG.registerInstructionHandler(beanFactory.getBean(HeartBeatRemoteErrorMsgHandler.class));
        Protocol.EXPEND.registerInstructionHandler(beanFactory.getBean(ExpendHandler.class));
        Protocol.SHRINK.registerInstructionHandler(beanFactory.getBean(ShrinkHandler.class));
    }



}
