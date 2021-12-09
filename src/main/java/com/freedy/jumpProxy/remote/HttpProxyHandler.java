package com.freedy.jumpProxy.remote;

import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ReleaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;


/**
 * @author Freedy
 * @date 2021/11/9 14:08
 */
@Slf4j
@Part(type = BeanType.PROTOTYPE)
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {

    @Inject
    private Bootstrap bootstrap;
    private String host;
    private int port;
    private HttpRequest request;

    public HttpProxyHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {
        final Channel remoteChannel = ctx.channel();
        this.request = request;
        initHostAndPort();

        log.info("[PROXY-{}]{} try connect to {}", request.method(), remoteChannel.remoteAddress(), host + ":" + port);
        Promise<Channel> promise = ctx.executor().newPromise();
        if (request.method().equals(HttpMethod.CONNECT)) {
            ReferenceCountUtil.release(request);
            promise.addListener((FutureListener<Channel>) future -> {
                if (!future.isSuccess()) {
                    responseError(remoteChannel);
                    ReleaseUtil.closeOnFlush(remoteChannel);
                    return;
                }
                final Channel serverChannel = future.getNow();
                responseSuccess(remoteChannel).addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        log.info("reply tunnel established Failed: " + ctx.channel().remoteAddress() + " " + request.method() + " " + request.uri());
                        ReleaseUtil.closeOnFlush(remoteChannel);
                        ReleaseUtil.closeOnFlush(serverChannel);
                        return;
                    }
                    ctx.pipeline().remove(HttpRequestDecoder.class);
                    ctx.pipeline().remove(HttpResponseEncoder.class);
                    ctx.pipeline().remove(HttpObjectAggregator.class);
                    ctx.pipeline().remove(HttpProxyHandler.class);
                    serverChannel.pipeline().addLast(new RelayHandler(remoteChannel));
                    ctx.pipeline().addLast(new RelayHandler(serverChannel));
                });
            });
        } else {
            promise.addListener((FutureListener<Channel>) future -> {
                if (!future.isSuccess()) {
                    responseError(remoteChannel);
                    ReleaseUtil.closeOnFlush(remoteChannel);
                    return;
                }
                final Channel serverChannel = future.getNow();
                ctx.pipeline().remove(HttpResponseEncoder.class);
                ctx.pipeline().remove(HttpProxyHandler.class);
                serverChannel.pipeline().addLast(new HttpRequestEncoder());
                serverChannel.pipeline().addLast(new RelayHandler(remoteChannel));
                RelayHandler relayHandler = new RelayHandler(serverChannel);
                ctx.pipeline().addLast(relayHandler);
                relayHandler.channelRead(ctx, request);
            });
        }

        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                responseError(remoteChannel);
                ReleaseUtil.closeOnFlush(remoteChannel);
                log.error("[CONNECT ERROR] cause:{}", future.cause().getMessage());
                promise.setFailure(future.cause());
            } else {
                promise.setSuccess(channelFuture.channel());
            }
        });

    }

    private void initHostAndPort() {
        String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
        port = Integer.parseInt(portStr);
    }

    private ChannelFuture responseError(Channel channel) {
        return channel.writeAndFlush(
                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
        );
    }

    private ChannelFuture responseSuccess(Channel channel) {
        return channel.writeAndFlush(
                new DefaultHttpResponse(request.protocolVersion(), new HttpResponseStatus(200, "Connection Established"))
        );
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        log.info("[EXCEPTION][" + clientHostname + "] " + cause.getMessage());
        ctx.close();
    }
}
