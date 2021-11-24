package com.freedy.jumpProxy.local;

import com.freedy.AuthenticAndDecrypt;
import com.freedy.AuthenticAndEncrypt;
import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ReleaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 仅初始化remoteChannel和消息转发
 *
 * @author Freedy
 * @date 2021/11/8 10:16
 */
@Slf4j
public class MsgForward extends ChannelInboundHandlerAdapter {

    private final Bootstrap bootstrap = new Bootstrap();
    private ChannelFuture connectFuture;

    private final LoadBalance<Struct.IpAddress> lb;
    private final boolean isProxy;
    private final int port;
    private String remoteAddress;
    private int remotePort;


    public MsgForward(LoadBalance<Struct.IpAddress> lb, boolean isProxy, int port) {
        this.lb = lb;
        this.isProxy = isProxy;
        this.port = port;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        bootstrap.group(localChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        if (isProxy) {
                            channel.pipeline().addLast(new LocalMsgForward(localChannel, port));
                        } else {
                            channel.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                    new LengthFieldPrepender(4),
                                    new AuthenticAndEncrypt(),
                                    new AuthenticAndDecrypt(null),
                                    new LocalMsgForward(localChannel, port)
                            );
                        }
                    }
                });
        //负载均衡
        Struct.IpAddress address = lb.getElement();
        remoteAddress = address.address();
        remotePort = address.port();
        lb.setAttributes(localChannel);
        connectFuture = bootstrap.connect(remoteAddress, remotePort);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        Channel localChannel = ctx.channel();
        connectFuture.addListener(future -> {
            if (!future.isSuccess()) {
                ErrorHandler.handle(ctx,msg);
                return;
            }
            Channel remoteChannel = connectFuture.channel();

            if (remoteChannel.isActive()) {
                //获取包信息
                String packInfo = null;
                if (msg instanceof ByteBuf byteBuf) {
                    packInfo = byteBuf.toString(Charset.defaultCharset());
                }

                //转发数据
                remoteChannel.writeAndFlush(msg);

                //日志输出
                if (isProxy) {
                    log.info("[REVERSE-PROXY]: redirect {} to {}", localChannel.remoteAddress().toString().substring(1), remoteAddress + ":" + remotePort);
                } else {
                    if (packInfo != null) {
                        Matcher matcher = Pattern.compile("(.*?) .*?HTTP/(.|" + Context.LF + ")*?(Host|host): (.*?)" + Context.LF + ".*").matcher(packInfo);
                        if (matcher.find()) {
                            //如果是http请求
                            log.info("[PROXY-HTTP-{}]: {} send msg to {}", matcher.group(1), localChannel.remoteAddress().toString().substring(1), matcher.group(4));
                        }
                    }
                }
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
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }

}
