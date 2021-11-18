package com.freedy.local;

import com.freedy.Context;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/8 10:20
 */
@Slf4j
public class LocalMsgForward extends ChannelInboundHandlerAdapter {
    private final Channel localChannel;

    public LocalMsgForward(Channel localChannel) {
        this.localChannel = localChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (localChannel.isActive()) {
            localChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("close connection from LOCAL[{}] to BROWSER[{}]", Context.LOCAL_PORT, localChannel.remoteAddress());
        ReleaseUtil.closeOnFlush(localChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[EXCEPTION] ==> {}", cause.getMessage());
    }
}
