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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * @author Freedy
 * @date 2021/11/6 16:19
 */
@Slf4j
public class LocalServer {


    public static void main(String[] args) throws InterruptedException {
        start(Context.JUMP_LOCAL_PORT, Context.JUMP_REMOTE_LB, false);
    }

    @SneakyThrows
    public static Channel start(int port, LoadBalance<Struct.IpAddress> lb, boolean isReverseProxy) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(0);

        byte[][] pac=new byte[1][];

        if (!isReverseProxy) {
            InputStream pacFile = MsgForward.class.getClassLoader().getResourceAsStream("pac");
            try {
                assert pacFile != null;
                pac[0] = pacFile.readAllBytes();
                log.info("pac server start success on url:http://127.0.0.1:{}/pac", port);
            } catch (Exception e) {
                log.error("pac server start failed!because {}", e.getMessage());
            }
        } else {
            pac[0] = null;
        }


        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_BACKLOG, 10240)
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new MsgForward(lb, isReverseProxy, port, pac[0]));
                    }
                });

        Channel channel = bootstrap.bind(port).sync().channel();
        log.info((isReverseProxy ? "ReverseProxy" : "local") + " service started success on port: {}", port);
        return channel;
    }
}
