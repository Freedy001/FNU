package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.TimeUnit;

/**
 * @author Freedy
 * @date 2021/11/17 15:50
 */
public class ClientInitHandler extends SimpleChannelInboundHandler<String> {

    private final Struct.SocketQuad group;
    private final AttributeKey<Struct.SocketQuad> groupInfo=AttributeKey.valueOf("groupInfo");

    public ClientInitHandler(Struct.SocketQuad group) {
        this.group = group;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith(Protocol.ACK)){
            Channel channel = ctx.channel();
            Attribute<Struct.SocketQuad> attr = channel.attr(groupInfo);
            attr.set(group);
            channel.writeAndFlush(Protocol.CONNECTION_SUCCESS_MSG);
            channel.pipeline().remove(ObjectEncoder.class);
            channel.pipeline().remove(ObjectDecoder.class);
            channel.pipeline().remove(ClientInitHandler.class);
            channel.pipeline().addLast(
                    new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME,0,0, TimeUnit.SECONDS),
                    new HeartBeatHandler(),
                    new RequestListener()
            );
            //重置坏连接次数
            IntranetClient.badConnectionTimes.set(0);
        }
    }
}
