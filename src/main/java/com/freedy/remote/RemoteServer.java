package com.freedy.remote;

import com.freedy.AuthenticAndDecrypt;
import com.freedy.AuthenticAndEncrypt;
import com.freedy.Context;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * @author Freedy
 * @date 2021/11/6 17:05
 */
public class RemoteServer {

    public static void main(String[] args) throws Exception {
        start();
    }

    public static Channel start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(0))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (Context.AES_KEY == null) {
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    new HttpProxyHandler()
                            );
                        } else {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                    new LengthFieldPrepender(4),
                                    new AuthenticAndEncrypt(),
                                    new AuthenticAndDecrypt(),
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    new HttpProxyHandler()
                            );
                        }
                    }
                });
        int port = Context.REMOTE_PORT;
        Channel channel = bootstrap.bind(port).sync().channel();
        System.out.println("remote server start on port:" + port);
        return channel;
    }


}
