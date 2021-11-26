package com.freedy.jumpProxy.remote;

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
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/6 17:05
 */
@Slf4j
public class RemoteServer {

    public static void main(String[] args) throws Exception {
        start(Context.JUMP_REMOTE_PORT, false);
    }

    public static Channel start(int port, boolean isHttpProxy) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(0))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (isHttpProxy) {
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
                                    new AuthenticAndDecrypt(null),
                                    new HttpRequestDecoder(),
                                    new HttpResponseEncoder(),
                                    new HttpObjectAggregator(Integer.MAX_VALUE),
                                    new HttpProxyHandler()
                            );
                        }
                    }
                });
        Channel channel = bootstrap.bind(port).sync().channel();
        log.info("remote server start on port:{}", port);
        return channel;
    }


}
