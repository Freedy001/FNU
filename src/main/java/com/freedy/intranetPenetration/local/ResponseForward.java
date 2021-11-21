package com.freedy.intranetPenetration.local;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;

/**
 * @author Freedy
 * @date 2021/11/17 15:20
 */
public class ResponseForward extends ChannelInboundHandlerAdapter {

    private final Channel remoteServerChannel;

    public ResponseForward(Channel channel) {
        remoteServerChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (remoteServerChannel.isActive()) {
            //无脑转发即可
            remoteServerChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }
}
