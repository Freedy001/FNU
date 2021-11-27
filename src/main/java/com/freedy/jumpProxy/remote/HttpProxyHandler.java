package com.freedy.jumpProxy.remote;

import com.freedy.jumpProxy.EmitPromise;
import com.freedy.utils.ReleaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;


/**
 * @author Freedy
 * @date 2021/11/9 14:08
 */
@Slf4j
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private final Bootstrap bootstrap = new Bootstrap();
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

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        System.out.log.println("[PROXY@"+request.method()+"] "+df.format(new Date())+" "+remoteChannel.remoteAddress()+" try to connect "+host + ":" + port);
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

        bootstrap.group(remoteChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new EmitPromise(promise))
                .connect(host, port).addListener(future -> {
                    if (!future.isSuccess()) {
                        responseError(remoteChannel);
                        ReleaseUtil.closeOnFlush(remoteChannel);
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
