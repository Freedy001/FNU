package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.Protocol;
import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * <h2>四次握手流程</h1>
 * <pre>
 *  第一次: 客户端像服务端发送配置组信息
 *         服务端更具分组信息将管道缓存起来
 *  第二次: 服务端返回ACK确认消息
 *         客户端对管道进行属性赋值
 *  第三次: 客户端发送CONNECT ESTABLISH SUCCEED!标识连接建立成功
 *         服务端初始化管道(增删相关ChannelHandler),根据配置在指定端口启动接收外部请求的服务
 *         并向客户端返回ACK
 *  第四次: 客户端初始化管道(增删相关ChannelHandler)，并开始在指定周期中发送心跳包
 * </pre>
 * <h2>工作流程</h2>
 * 管道初始化完成后,客户端开始监听服务端的请求,并将请求转发到本地服务.<br/>
 * 本地服务返回数据,客户端将数据转发到服务端，服务端再转发给用户.
 * @author Freedy
 * @date 2021/11/17 15:50
 */
@Slf4j
public class ClientHandshake extends SimpleChannelInboundHandler<String> {

    private final Struct.ConfigGroup group;
    private boolean isFirst = true;

    public ClientHandshake(Struct.ConfigGroup group) {
        this.group = group;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith(Protocol.ACK)) {
            log.debug("[client]接收ACK");
            Channel channel = ctx.channel();
            if (isFirst) {
                ChannelUtils.setGroup(channel, group);
                log.debug("[client]发送CONNECT ESTABLISH SUCCEED!");
                channel.writeAndFlush(Protocol.CONNECTION_SUCCESS_MSG);
                isFirst=false;
            } else {
                channel.pipeline().remove(ObjectEncoder.class);
                channel.pipeline().remove(ObjectDecoder.class);
                channel.pipeline().remove(ClientHandshake.class);
                channel.pipeline().addLast(
                        new IdleStateHandler(Context.INTRANET_READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS),
                        new HeartBeatHandler(),
                        new RequestListener()
                );
                ChannelUtils.setInit(channel,true);
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("[EXCEPTION]: " + cause.getMessage());
    }
}
