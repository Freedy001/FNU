package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理服务端心跳相应
 *
 * @author Freedy
 * @date 2021/11/18 16:14
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private int readIdleTimes = 0;


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.READER_IDLE) {
                readIdleTimes++; // 读空闲的计数加1
                log.debug("读空闲[{}]", readIdleTimes);
            }

            if (readIdleTimes > Context.INTRANET_READER_IDLE_TIMES) { // 读空闲超时
                readIdleTimes = 0;
                handleBadChannel(ctx);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        handleBadChannel(ctx);
    }

    private void handleBadChannel(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("[INTRANET-LOCAL-SERVER]: 关闭管道{}", channel.toString());
        Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
        ClientConnector.remoteChannelMap.get(group).remove(channel);
        channel.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }

}
