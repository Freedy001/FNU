package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;

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
    public void channelRead(ChannelHandlerContext ctx, Object byteBuf) throws InterruptedException {
        if (byteBuf instanceof ByteBuf msg) {
            String packMsg = msg.toString(Charset.defaultCharset());
            if (packMsg.startsWith(Protocol.HEARTBEAT_REMOTE_NORMAL_MSG)) {
//                log.debug("[LOCAL-HEART-RECEIVE]: {}",packMsg);
                msg.release();
                // do nothing
                return;
            } else if (packMsg.startsWith(Protocol.HEARTBEAT_REMOTE_ERROR_MSG)) {
//                log.debug("[LOCAL-HEART-RECEIVE]: {}",packMsg);
                msg.release();
                //重新开启一个管道
                String inactiveAddress = packMsg.split("!")[1];
                Struct.IpAddress inActiveIP = ChannelUtils.parseAddress(inactiveAddress);
                if (inActiveIP == null) {
                    return;  // do nothing
                }

                List<Channel> channelList = ClientConnector.remoteChannelMap.get(ChannelUtils.getGroup(ctx.channel()));
                for (int i = 0; i < channelList.size(); i++) {
                    Struct.IpAddress ipAddress = ChannelUtils.parseAddress(channelList.get(i).localAddress());
                    if (inActiveIP.portEqual(ipAddress)){
                        //找到失去活性的channel
                        channelList.remove(i);
                        break;
                    }
                }

                return;
            }
        }
        ctx.fireChannelRead(byteBuf);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.READER_IDLE) {
                readIdleTimes++; // 读空闲的计数加1
            }

            if (readIdleTimes > Context.INTRANET_READER_IDLE_TIMES) { // 读空闲超时
                channelInactive(ctx);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("关闭管道: {}",channel.toString());
        Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
        ClientConnector.remoteChannelMap.get(group).remove(channel);
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }

}
