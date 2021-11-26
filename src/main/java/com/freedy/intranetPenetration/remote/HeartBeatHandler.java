package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

/**
 * 处理客户端心跳
 *
 * @author Freedy
 * @date 2021/11/18 16:14
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    int readIdleTimes = 0;


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        requireNewChannelAndDeleteOld(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ConnectException) {
            requireNewChannelAndDeleteOld(ctx);
        }
//        cause.printStackTrace();
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.READER_IDLE) {
                readIdleTimes++; // 读空闲的计数加1
                log.debug("读空闲[{}]",readIdleTimes);
            }

            if (readIdleTimes >= Context.INTRANET_READER_IDLE_TIMES) {
                readIdleTimes = 0;
                requireNewChannelAndDeleteOld(ctx);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void requireNewChannelAndDeleteOld(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        int port = ChannelUtils.getGroup(localChannel).getRemoteServerPort();
        LoadBalance<Channel> balance = ChanelWarehouse.PORT_CHANNEL_CACHE.get(port);
        balance.removeElement(localChannel);
        localChannel.close();
        log.info("[INTRANET-REMOTE-SERVER]: 关闭管道({})", localChannel);
        //如果该管道是服务端主动销毁，则不需要发送检测消息
        if (ChannelUtils.getDestroyState(localChannel)) return;
        //通过下一个channel像客户端索要一个新的channel
        Channel nextChannel = balance.getElement();
        if (nextChannel == null) {
            return;
        }
        ChannelUtils.setCmdAndSend(nextChannel, Protocol.HEARTBEAT_REMOTE_ERROR_MSG);
    }
}
