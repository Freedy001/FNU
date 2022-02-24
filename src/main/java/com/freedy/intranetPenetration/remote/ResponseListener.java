package com.freedy.intranetPenetration.remote;

import com.freedy.intranetPenetration.OccupyState;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * 转发来自本地客户端的消息
 * @author Freedy
 * @date 2021/11/17 17:01
 */
@Slf4j
@ChannelHandler.Sharable
@Part(type = BeanType.SINGLETON)
public class ResponseListener extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel intranetChannel = ctx.channel();
        OccupyState state = ChannelUtils.getOccupy(intranetChannel);
        Channel receiverChannel = state.getReceiverChannel();


        if (receiverChannel != null && receiverChannel.isActive()) {
            receiverChannel.writeAndFlush(msg);
        } else {
            log.warn("A message will be discarded,because the receiverChannel is already release!");
            ReleaseUtil.release(msg);
        }
    }

}
