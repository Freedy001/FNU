package com.freedy.jumpProxy.local;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.loadBalancing.LoadBalance;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/6 16:19
 */
@Slf4j
public class LocalServer {


    public static void main(String[] args) throws InterruptedException {
        start(Context.JUMP_LOCAL_PORT, Context.JUMP_REMOTE_LB, false);
    }

    public static Channel start(int port, LoadBalance<Struct.IpAddress> lb, boolean isReverseProxy) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(0);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new MsgForward(lb, isReverseProxy,port));
                    }
                });

        Channel channel = bootstrap.bind(port).sync().channel();
        log.info((isReverseProxy ? "ReverseProxy" : "local") + " service started success on port: {}", port);
        return channel;
    }
}
