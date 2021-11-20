package com.freedy.jumpProxy.local;

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
    private final int port;

    public LocalMsgForward(Channel localChannel,int port) {
        this.localChannel = localChannel;
        this.port=port;
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
        log.debug("close connection from LOCAL[{}] to BROWSER[{}]", port, localChannel.remoteAddress());
        ReleaseUtil.closeOnFlush(localChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[EXCEPTION] ==> {}", cause.getMessage());
    }
}
