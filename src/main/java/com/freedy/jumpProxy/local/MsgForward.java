package com.freedy.jumpProxy.local;

import com.freedy.*;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.jumpProxy.ReverseProxyProp;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ReleaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private final byte[] pacData;

    @Inject
    private EncryptProp encryptProp;

    private String remoteAddress;
    private int remotePort;

    public MsgForward(ReverseProxyProp proxyProp,@Inject("pac") byte[] pacData) {
        this.lb = proxyProp.getReverseProxyLB();
        this.port = proxyProp.getPort();
        this.jumpEndPoint = proxyProp.getJumpEndPoint();
        this.pacData = pacData;
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
                        new AuthenticAndEncrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken()),
                        new AuthenticAndDecrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken(),null),
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

        //获取包信息
        String packInfo = null;
        if (msg instanceof ByteBuf byteBuf) {
            packInfo = byteBuf.toString(Charset.defaultCharset());
        }
        //日志输出
        if (jumpEndPoint) {
            if (packInfo != null) {
                Matcher matcher = Pattern.compile("(.*?) (.*?)HTTP/(.|" + Context.LF + ")*?(Host|host): (.*?)" + Context.LF + ".*").matcher(packInfo);
                if (matcher.find()) {
                    //如果是http请求
                    String host = matcher.group(5);
                    if (host.equals("127.0.0.1:"+port)||host.equals("localhost:"+port)) {
                        ctx.channel().pipeline().addLast(new HttpResponseEncoder());
                        String url = matcher.group(2).trim();
                        if (!url.equals("/pac")) {
                            log.error("illegal url: {}", url);
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            return;
                        }
                        if (pacData == null) {
                            log.error("pac start failed,please check config!");
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            return;
                        }
                        log.info("{} request pac", localChannel.remoteAddress().toString().substring(1));
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-ns-proxy-autoconfig");
                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, pacData.length);
                        response.content().writeBytes(pacData);
                        ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        return;
                    }
                    log.info("[PROXY-HTTP-{}]: {} send msg to {}", matcher.group(1), localChannel.remoteAddress().toString().substring(1), host);
                }
            }
        } else {
            log.info("[REVERSE-PROXY]: redirect {} to {}", localChannel.remoteAddress().toString().substring(1), remoteAddress + ":" + remotePort);
        }

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
//            cause.printStackTrace();
            log.error("[EXCEPTION]: {}", cause.getMessage());
        }
    }

}
