package com.freedy.intranetPenetration.remote;

import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/17 17:01
 */
@Slf4j
public class ResponseListener extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel intranetChannel = ctx.channel();
        Channel receiverChannel = RequestReceiver.intranetReceiverMap.get(intranetChannel);

        if (receiverChannel.isActive()){
            receiverChannel.writeAndFlush(msg);
            OccupyState state = ChannelUtils.getOccupy(intranetChannel);



        }


        super.channelRead(ctx, msg);
    }





}
