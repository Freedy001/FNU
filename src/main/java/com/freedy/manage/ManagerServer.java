package com.freedy.manage;

import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.Application;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static com.freedy.Context.MANAGE_PORT;

/**
 * @author Freedy
 * @date 2021/11/29 9:28
 */
@Slf4j
@Part
public class ManagerServer {

    @Inject
    Application app;

    public static void main(String[] args) {
        new ManagerServer().start();
    }

    @SneakyThrows
    public Channel start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        Channel channel = bootstrap.group(new NioEventLoopGroup(1),
                        new NioEventLoopGroup(0, new DefaultThreadFactory("manager-server")))
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpRequestDecoder(),
                                new HttpResponseEncoder(),
                                new HttpObjectAggregator(Integer.MAX_VALUE),
                                new Test()
                        );
                    }
                })
                .bind(MANAGE_PORT).sync().channel();

        log.info("manager server start succeed on address: http://127.0.0.1:{}/", MANAGE_PORT);
        return channel;
    }

}
