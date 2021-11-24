package com.freedy.intranetPenetration.local;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/17 15:20
 */
@Slf4j
public class ResponseForward extends ChannelInboundHandlerAdapter {

    private final Channel remoteServerChannel;

    public ResponseForward(Channel channel) {
        remoteServerChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (remoteServerChannel.isActive()) {
            //无脑转发即可
            remoteServerChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }
}
