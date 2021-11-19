package com.freedy.errorProcessor;

import com.freedy.Protocol;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * @author Freedy
 * @date 2021/11/18 11:25
 */
public class ErrorHandler {

    private final static byte[] errPage;

    static {
        InputStream stream = ErrorHandler.class.getClassLoader().getResourceAsStream("ErrorPage.html");
        try {
            assert stream != null;
            errPage = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handle(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        channel.pipeline().addLast(new HttpResponseEncoder());
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);

        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, errPage.length);
        response.content().writeBytes(errPage);
        ReleaseUtil.release(msg);
        ReleaseUtil.closeOnFlush(channel);
    }

    public static void LocalServerErr(ChannelHandlerContext ctx, Object msg){
        ChannelUtils.sendString(ctx, Protocol.HEARTBEAT_LOCAL_ERROR_MSG);
        ReleaseUtil.release(msg);
    }


}
