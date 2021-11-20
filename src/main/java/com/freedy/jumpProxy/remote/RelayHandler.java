package com.freedy.jumpProxy.remote;

import com.freedy.utils.ReleaseUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author Freedy
 * @date 2021/11/11 11:52
 */
@Slf4j
public class RelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel relayChannel;

    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        boolean canWrite = ctx.channel().isWritable();
        relayChannel.config().setAutoRead(canWrite);
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest request) {//删除代理特有的请求头
            request.headers().remove("Proxy-Authorization");
            String proxyConnection = request.headers().get("Proxy-Connection");
            if (Objects.nonNull(proxyConnection)) {
                request.headers().set("Connection", proxyConnection);
                request.headers().remove("Proxy-Connection");
            }

            //获取Host和port
            String hostAndPortStr = request.headers().get("Host");
            String[] hostPortArray = hostAndPortStr.split(":");
            String host = hostPortArray[0];
            String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
            int port = Integer.parseInt(portStr);

            try {
                String url = request.uri();
                int index = url.indexOf(host) + host.length();
                url = url.substring(index);
                if (url.startsWith(":")) {
                    url = url.substring(1 + String.valueOf(port).length());
                }
                request.setUri(url);
            } catch (Exception e) {
                System.err.println("无法获取url：" + request.uri() + " " + host);
            }
        }

        if (relayChannel.isActive()) {
            relayChannel.writeAndFlush(msg).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("relay error!", future.cause());
                }
            });
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("close connection from {} to {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        ReleaseUtil.closeOnFlush(relayChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }

}
