package com.freedy.intranetPenetration.local;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;

/**
 * @author Freedy
 * @date 2021/11/17 14:46
 */
public class RequestListener extends ChannelInboundHandlerAdapter {

    private final Bootstrap bootstrap = new Bootstrap();
    private Channel channel;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new ResponseForward(ctx.channel()));
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (channel == null) {
            initChannel(ctx, msg);
        } else {
            if (channel.isActive()) {
                channel.writeAndFlush(msg);
            } else {
                initChannel(ctx, msg);
            }
        }
    }

    private void initChannel(ChannelHandlerContext ctx, Object msg) {
        final int[] time = {0};
        ChannelFutureListener futureListener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    Channel serverChannel = channelFuture.channel();
                    serverChannel.writeAndFlush(msg);
                    channel = serverChannel;
                } else {
                    if (time[0] == 3) {
                        ctx.channel().writeAndFlush(Unpooled.wrappedBuffer("CONNECT ERROR!".getBytes(StandardCharsets.UTF_8)));
                        ReferenceCountUtil.release(msg);
                        return;
                    }
                    time[0]++;
//                    bootstrap.connect(Context.INTRANET_PENETRATION_LOCAL_SERVER_ADDRESS, Context.INTRANET_PENETRATION_LOCAL_SERVER_PORT)
//                            .addListener(this);
                }
            }
        };
//        bootstrap.connect(Context.INTRANET_PENETRATION_LOCAL_SERVER_ADDRESS, Context.INTRANET_PENETRATION_LOCAL_SERVER_PORT)
//                .addListener(futureListener);
    }



    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
