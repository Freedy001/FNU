package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.intranetPenetration.ForwardTask;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!isHeartBeatPack(ctx, (ByteBuf) msg)) {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean isHeartBeatPack(ChannelHandlerContext ctx, ByteBuf msg) {
        String info = msg.toString(Charset.defaultCharset());
        if (info.startsWith(Protocol.HEARTBEAT_LOCAL_NORMAL_MSG)) {
            ChannelUtils.sendString(ctx.channel(), Protocol.HEARTBEAT_REMOTE_NORMAL_MSG);
            ReleaseUtil.release(msg);
            return true;
        }
        return false;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        requireNewChannelAndDeleteOld(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ConnectException) {
            requireNewChannelAndDeleteOld(ctx);
        }
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
        //通过下一个channel像客户端索要一个新的channel
        Channel nextChannel = balance.getElement();
        if (nextChannel == null) {
            return;
        }
        ChannelUtils.sendString(nextChannel, Protocol.HEARTBEAT_REMOTE_ERROR_MSG + localChannel.remoteAddress().toString());
    }
}
