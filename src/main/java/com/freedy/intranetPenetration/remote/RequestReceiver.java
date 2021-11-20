package com.freedy.intranetPenetration.remote;

import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.intranetPenetration.ForwardTask;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/11/17 21:13
 */
@Slf4j
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
    public void channelActive(ChannelHandlerContext ctx) {
        intranetChannel = lb.getElement();
        if (intranetChannel != null) {
            //打印日志
            Struct.ConfigGroup group = ChannelUtils.getGroup(intranetChannel);
            log.info("Redirect {} to {} for request {}", ctx.channel().remoteAddress().toString().substring(1), intranetChannel.remoteAddress().toString().substring(1), group.getLocalServerAddress() + ":" + group.getLocalServerPort());
        }

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
            //System.out.println(state);
            if (state.tryOccupy(receiverChannel)){
                //转发信息
                //System.out.println("转发消息");
                intranetChannel.writeAndFlush(msg);
                intranetReceiverMap.put(intranetChannel, receiverChannel);
                receiverIntranetMap.put(receiverChannel, intranetChannel);
            }else {         //管道繁忙，尝试其他管道
                if (changeTimes >= lb.size()) {
                    //提交一个任务 等待其他的任务执行完
                    //System.out.println("提交一个任务 等待其他的任务执行完");
                    state.submitTask(new ForwardTask());
                    changeTimes = 0;
                }
                //切换管道
                //System.out.println("管道繁忙,切换管道");
                intranetChannel = lb.getElement();
                changeTimes++;
                channelRead(ctx, msg);
            }
        }else {
            if (retryCount >= lb.size()) {
                //尝试失败次数大于临界值
                //System.out.println("尝试失败次数大于临界值");
                ErrorHandler.handle(ctx, msg);
            }
            retryCount++;
            //切换管道
            //System.out.println("管道被关闭,切换管道");
            intranetChannel = lb.getElement();
            channelRead(ctx,msg);
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelUtils.getOccupy(intranetChannel).release(ctx.channel());
    }
}
