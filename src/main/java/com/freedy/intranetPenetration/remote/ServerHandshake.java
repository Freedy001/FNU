package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.utils.ChannelUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Freedy
 * @date 2021/11/17 17:57
 */
@Slf4j
public class ServerHandshake extends SimpleChannelInboundHandler<String> {

    public final static Map<Integer,Channel> PORT_STARTED=new ConcurrentHashMap<>();

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
                                new HeartBeatHandler(),
                                new ResponseListener()
                        );
                        channel.pipeline().addFirst(
                                new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS)
                        );
                    }
                });

            }
            Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
            final int remoteServerPort = group.getRemoteServerPort();
            if (PORT_STARTED.put(remoteServerPort, fakeChannel)==null) {
                //启动服务
                ServerBootstrap bootstrap = new ServerBootstrap();
                Channel parentChannel = bootstrap.group(new NioEventLoopGroup(1),
                                IntranetServer.workGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 10240)
                        .childHandler(new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                channel.pipeline().addLast(
                                        new RequestReceiver(remoteServerPort)
                                );
                            }
                        })
                        .bind(remoteServerPort).sync().channel();
                PORT_STARTED.put(remoteServerPort, parentChannel);
                System.out.println("Intranet-Slave-Server started success on port:" + remoteServerPort);
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        log.info("[EXCEPTION]: " + cause.getMessage());
    }

    //ConcurrentHashMap 的value不能为空,所以用这个Channel来代替空值
    public static final Channel fakeChannel=new Channel(){

        @Override
        public ChannelId id() {
            return null;
        }

        @Override
        public EventLoop eventLoop() {
            return null;
        }

        @Override
        public Channel parent() {
            return null;
        }

        @Override
        public ChannelConfig config() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isRegistered() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public ChannelMetadata metadata() {
            return null;
        }

        @Override
        public SocketAddress localAddress() {
            return null;
        }

        @Override
        public SocketAddress remoteAddress() {
            return null;
        }

        @Override
        public ChannelFuture closeFuture() {
            return null;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public long bytesBeforeUnwritable() {
            return 0;
        }

        @Override
        public long bytesBeforeWritable() {
            return 0;
        }

        @Override
        public Channel.Unsafe unsafe() {
            return null;
        }

        @Override
        public ChannelPipeline pipeline() {
            return null;
        }

        @Override
        public ByteBufAllocator alloc() {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture disconnect() {
            return null;
        }

        @Override
        public ChannelFuture close() {
            return null;
        }

        @Override
        public ChannelFuture deregister() {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture close(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture deregister(ChannelPromise promise) {
            return null;
        }

        @Override
        public Channel read() {
            return null;
        }

        @Override
        public ChannelFuture write(Object msg) {
            return null;
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            return null;
        }

        @Override
        public Channel flush() {
            return null;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            return null;
        }

        @Override
        public ChannelPromise newPromise() {
            return null;
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            return null;
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            return null;
        }

        @Override
        public ChannelFuture newFailedFuture(Throwable cause) {
            return null;
        }

        @Override
        public ChannelPromise voidPromise() {
            return null;
        }

        @Override
        public <T> Attribute<T> attr(AttributeKey<T> key) {
            return null;
        }

        @Override
        public <T> boolean hasAttr(AttributeKey<T> key) {
            return false;
        }

        @Override
        public int compareTo(Channel o) {
            return 0;
        }
    };
}
