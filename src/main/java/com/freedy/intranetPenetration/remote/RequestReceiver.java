package com.freedy.intranetPenetration.remote;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/11/17 21:13
 */
public class RequestReceiver extends ChannelInboundHandlerAdapter {

    private final LoadBalance<Channel> lb;
    private Channel intranetChannel;
    private int retryCount=0;
    private int changeTimes=0;
    public static final Map<Channel,Channel> intranetReceiverMap=new ConcurrentHashMap<>();
    public static final Map<Channel,Channel> receiverIntranetMap=new ConcurrentHashMap<>();

    public RequestReceiver(int remotePort) {
        lb=ChanelWarehouse.PORT_CHANNEL_CACHE.get(remotePort);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
         intranetChannel = lb.getElement();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (intranetChannel==null){
            ErrorHandler.handle(ctx,msg);
            return;
        }
        Channel receiverChannel = ctx.channel();
        if (intranetChannel.isActive()){
            retryCount=0; // 重置连接失败次数

            OccupyState state = ChannelUtils.getOccupy(intranetChannel);

            if (state.tryOccupy(receiverChannel)){
                //转发信息
                intranetChannel.writeAndFlush(msg);
                intranetReceiverMap.put(intranetChannel, receiverChannel);
                receiverIntranetMap.put(receiverChannel, intranetChannel);
            }else {         //管道繁忙，尝试其他管道
                if (changeTimes>=lb.size()){
                    //等待
                    Promise<Channel> promise = ctx.executor().newPromise();
                    promise.addListener((FutureListener<Channel>) future->{
                        if (future.isSuccess()){
                            Channel futureChannel = future.getNow();
                            futureChannel.writeAndFlush(msg);
                        }
                    });
                    state.submitTask(promise);
                }
                //切换管道
                intranetChannel = lb.getElement();
                changeTimes++;
                channelRead(ctx,msg);
            }
        }else {
            if (retryCount>=Context.INTRANET_CHANNEL_RETRY_TIME){
                //尝试失败次数大于临界值
                ErrorHandler.handle(ctx,msg);
                ReleaseUtil.release(msg);
            }
            retryCount++;
            //切换管道
            intranetChannel = lb.getElement();
            channelRead(ctx,msg);
        }

    }
}
