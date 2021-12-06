package com.freedy.manage;

import com.freedy.log.LogRecorder;
import com.freedy.manage.entity.A;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @author Freedy
 * @date 2021/12/3 17:48
 */
@Part
public class Test extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Inject
    private A a;

    @PostConstruct
    public void postConstruct(){
        System.out.println("postConstruct 被调用");
        System.out.println(a);
    }

    @Inject
    public void testInject(LogRecorder recorder){
        System.out.println("inject method invoked");
        System.err.println(recorder.getLog());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (msg.uri().equals("/")) {
            System.out.println("相应普通");
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(CONTENT_TYPE, "text/html");
            response.headers().set(CONNECTION, "keep-alive");
            response.headers().set(SERVER, "FNU power by netty");
            response.headers().set(DATE, new Date());
            String val = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta http-equiv="X-UA-Compatible" content="IE=edge">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Document</title>
                    </head>
                    <body>
                        <video controls="" autoplay="" name="media"><source src="/music" type="audio/mpeg"></video>
                    </body>
                    </html>
                    """;
            //C:/Users/Freedy/Music/ifWeHaveEachOther.mp3
            byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
            response.headers().set(CONTENT_LENGTH, bytes.length);
            response.content().writeBytes(bytes);
            ctx.writeAndFlush(response);
        } else if (msg.uri().equals("/music")) {
            System.out.println("相应音乐");
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(CONTENT_TYPE, "audio/mp3");
            response.headers().set(CONNECTION, "keep-alive");
            response.headers().set(SERVER, "FNU power by netty");
            response.headers().set(DATE, new Date());
            FileInputStream stream = new FileInputStream("C:/Users/Freedy/Music/ifWeHaveEachOther.mp3");
            byte[] bytes = stream.readAllBytes();
            //C:/Users/Freedy/Music/ifWeHaveEachOther.mp3
            response.headers().set(CONTENT_LENGTH, bytes.length);
            response.content().writeBytes(bytes);
            ctx.writeAndFlush(response);
        }
    }

}
