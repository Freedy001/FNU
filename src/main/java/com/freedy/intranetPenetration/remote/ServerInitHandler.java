package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author Freedy
 * @date 2021/11/17 17:57
 */
public class ServerInitHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        if (msg.startsWith(Protocol.CONNECTION_SUCCESS_MSG)){
            Channel channel = channelHandlerContext.channel();
            channel.pipeline().remove(ObjectEncoder.class);
            channel.pipeline().remove(ObjectDecoder.class);
            channel.pipeline().remove(ChanelWarehouse.class);
            channel.pipeline().remove(ServerInitHandler.class);
            channel.pipeline().addLast(
                    new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME,0,0, TimeUnit.SECONDS),
                    new HeartBeatHandler(),
                    new ResponseListener()
            );
        }
    }
}
