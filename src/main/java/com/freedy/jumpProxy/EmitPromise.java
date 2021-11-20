package com.freedy.jumpProxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/11 16:01
 */
@AllArgsConstructor
@Slf4j
public class EmitPromise extends ChannelInboundHandlerAdapter{

    private final Promise<Channel> promise;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        log.error("[EmitError] cause:{}",throwable.getMessage());
        promise.setFailure(throwable);
    }
}
