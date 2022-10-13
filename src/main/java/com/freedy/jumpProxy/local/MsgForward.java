package com.freedy.jumpProxy.local;

import com.freedy.AuthenticAndDecrypt;
import com.freedy.AuthenticAndEncrypt;
import com.freedy.EncryptProp;
import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.jumpProxy.ReverseProxyProp;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ReleaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;


/**
 * 仅初始化remoteChannel和消息转发
 *
 * @author Freedy
 * @date 2021/11/8 10:16
 */
@Part(type = BeanType.PROTOTYPE)
@Slf4j
public class MsgForward extends ChannelInboundHandlerAdapter {

    private ChannelFuture connectFuture;

    @Inject
    private Bootstrap bootstrap;

    private final LoadBalance<Struct.IpAddress> lb;
    private final int port;
    private final boolean jumpEndPoint;

    @Inject
    private EncryptProp encryptProp;

    private String remoteAddress;
    private int remotePort;

    public MsgForward(ReverseProxyProp proxyProp) {
        this.lb = proxyProp.getReverseProxyLB();
        this.port = proxyProp.getPort();
        this.jumpEndPoint = proxyProp.getJumpEndPoint();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        //负载均衡
        Struct.IpAddress address = lb.getElement();
        remoteAddress = address.address();
        remotePort = address.port();
        lb.setAttributes(localChannel);
        connectFuture = bootstrap.connect(remoteAddress, remotePort);
        connectFuture.addListener(future -> {
            Channel channel = connectFuture.channel();
            if (jumpEndPoint) {
                channel.pipeline().addLast(
                        new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                        new LengthFieldPrepender(4),
                        new AuthenticAndEncrypt(encryptProp.getAesKey(), encryptProp.getAuthenticationToken()),
                        new AuthenticAndDecrypt(encryptProp.getAesKey(), encryptProp.getAuthenticationToken(), null),
                        new LocalMsgForward(localChannel, port)
                );
            } else {
                channel.pipeline().addLast(new LocalMsgForward(localChannel, port));
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel localChannel = ctx.channel();
        log.info("[REVERSE-PROXY]: redirect {} to {}", localChannel.remoteAddress().toString().substring(1), remoteAddress + ":" + remotePort);
        connectFuture.addListener(future -> {
            if (!future.isSuccess()) {
                ErrorHandler.handle(ctx, msg);
                return;
            }
            Channel remoteChannel = connectFuture.channel();

            if (remoteChannel.isActive()) {
                //转发数据
                remoteChannel.writeAndFlush(msg);
            } else {
                ReleaseUtil.release(msg);
            }
        });
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("close connection from LOCAL[{}] to REMOTE[{}]", port, remoteAddress + ":" + remotePort);
        ReleaseUtil.closeOnFlush(connectFuture.sync().channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ConnectException) {
            ErrorHandler.handle(ctx, null);
        } else {
            log.error("[EXCEPTION]: {}", cause.getMessage());
        }
    }

}
