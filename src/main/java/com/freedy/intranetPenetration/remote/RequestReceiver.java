package com.freedy.intranetPenetration.remote;

import com.freedy.Struct;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.intranetPenetration.ForwardTask;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/11/17 21:13
 */
@Slf4j
@Part(type = BeanType.PROTOTYPE)
public class RequestReceiver extends ChannelInboundHandlerAdapter {
    @Setter
    private LoadBalance<Channel> lb;
    private Channel intranetChannel;
    private int retryCount = 0;
    private int changeTimes = 0;
    public static final Map<Channel, Channel> intranetReceiverMap = new ConcurrentHashMap<>();
    public static final Map<Channel, Channel> receiverIntranetMap = new ConcurrentHashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        intranetChannel = lb.getElement();
        if (intranetChannel != null) {
            //打印日志
            Struct.ConfigGroup group = ChannelUtils.getGroup(intranetChannel);
            log.info("[INTRANET-REMOTE-PROXY]: Preparing to redirect {} to {} for request {}", ctx.channel().remoteAddress().toString().substring(1), intranetChannel.remoteAddress().toString().substring(1), group.getLocalServerAddress() + ":" + group.getLocalServerPort());
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (intranetChannel == null) {
            ErrorHandler.handle(ctx, msg);
            return;
        }
        Channel receiverChannel = ctx.channel();
        if (intranetChannel.isActive()) {
            retryCount = 0; // 重置连接失败次数

            OccupyState state = ChannelUtils.getOccupy(intranetChannel);

            if (state.tryOccupy(receiverChannel)){
                OccupyState.inspectChannelState();
                //转发信息
                intranetChannel.writeAndFlush(msg);
                intranetReceiverMap.put(intranetChannel, receiverChannel);
                receiverIntranetMap.put(receiverChannel, intranetChannel);
            }else {         //管道繁忙，尝试其他管道
                if (changeTimes >= lb.size()) {
                    //提交一个任务 等待其他的任务执行完

                    state.submitTask(new ForwardTask(receiverChannel, msg));
                    changeTimes = 0;
                    OccupyState.inspectChannelState();
                    return;
                }

                //切换管道
                intranetChannel = lb.getElement();
                changeTimes++;
                channelRead(ctx, msg);
            }
        }else {
            if (retryCount >= lb.size()) {
                //尝试失败次数大于临界值
                ErrorHandler.handle(ctx, msg);
            }
            retryCount++;
            //切换管道
            intranetChannel = lb.getElement();
            channelRead(ctx, msg);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (intranetChannel != null)
            ChannelUtils.getOccupy(intranetChannel).release(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
        log.error("[EXCEPTION]: {}", cause.getMessage());
    }
}
