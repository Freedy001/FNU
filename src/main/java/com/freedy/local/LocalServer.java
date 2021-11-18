package com.freedy.local;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.loadBalancing.LoadBalance;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Freedy
 * @date 2021/11/6 16:19
 */
public class LocalServer {

    public static void main(String[] args) throws InterruptedException {
        start(Context.PROXY_PORT, Context.PROXY_LB, true);
    }

    public static Channel start(int port, LoadBalance<Struct.IpAddress> lb, boolean isProxy) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(0);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new MsgForward(lb, isProxy));
                    }
                });

        Channel channel = bootstrap.bind(port).sync().channel();
        System.out.println((isProxy ? "proxy" : "local") + " service started success on port:" + port);
        return channel;
    }
}
