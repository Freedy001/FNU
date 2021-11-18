package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * @author Freedy
 * @date 2021/11/17 21:13
 */
public class RequestReceiver extends ChannelInboundHandlerAdapter {

    private final int port;
    private final AttributeKey<Struct.OccupyState> occupied =AttributeKey.valueOf("occupied");
    private Channel intranetChannel;
    private int retryCount=0;

    public RequestReceiver(int remotePort) {
        port=remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
         intranetChannel = ChanelWarehouse.PORT_CHANNEL_CACHE.get(port).getElement();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (intranetChannel==null){
            ErrorHandler.handle(ctx,msg);
            return;
        }
        Channel receiverChannel = ctx.channel();
        if (intranetChannel.isActive()){
            Attribute<Struct.OccupyState> attr = intranetChannel.attr(occupied);
            Struct.OccupyState hasOccupy = attr.get();
            if (hasOccupy ==null){
                attr.set(hasOccupy=new Struct.OccupyState(true,receiverChannel,new ArrayList<>()));
            }

            if (!hasOccupy.occupied()||hasOccupy.receiverChannel()==receiverChannel){
                //转发信息
                intranetChannel.writeAndFlush(msg);
            }else {
                //等待
                Promise<Channel> promise = ctx.executor().newPromise();
                promise.addListener((FutureListener<Channel>) future->{
                    if (future.isSuccess()){
                        Channel futureChannel = future.getNow();
                        futureChannel.writeAndFlush(msg);
                    }
                });
                hasOccupy.wakeupList().add(promise);
            }

        }else {
            if (retryCount>=Context.INTRANET_CHANNEL_RETRY_TIME){
                //尝试失败次数大于临界值
                ReferenceCountUtil.release(msg);
                receiverChannel.writeAndFlush(
                        Unpooled.copiedBuffer("404 Not Found", Charset.defaultCharset())
                );
            }
            retryCount++;
            //切换管道
            intranetChannel = ChanelWarehouse.PORT_CHANNEL_CACHE.get(port).getElement();
            channelRead(ctx,msg);
        }

    }
}
