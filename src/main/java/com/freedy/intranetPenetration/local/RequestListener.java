package com.freedy.intranetPenetration.local;

import com.freedy.Context;
import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/17 14:46
 */
@Slf4j
public class RequestListener extends ChannelInboundHandlerAdapter {

    private Channel localServerChannel;
    private int spin=0;
    private long lastCircleTime=System.currentTimeMillis();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel remoteChannel = ctx.channel();
        localServerChannel = ClientConnector.localServerConnect(ChannelUtils.getGroup(remoteChannel), remoteChannel);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        for (int retryCount = 0; localServerChannel == null; retryCount++) {
            if (retryCount >= Context.INTRANET_CHANNEL_RETRY_TIME) {
                ErrorHandler.LocalServerErr(ctx,msg);
                return;
            }
            channelActive(ctx);
        }

        if (localServerChannel.isActive()) {
            localServerChannel.writeAndFlush(msg);
        } else {
            //该handle是长连接所以必须通过单位时间来的自旋次数来判断是否本地服务出错
            spin++;
            if (spin%100==0){
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis-lastCircleTime<30_000){
                    log.warn("detect suspicious spin which 100 times in 30 second");
                    ErrorHandler.LocalServerErr(ctx,msg);
                    return;
                }
                lastCircleTime=currentTimeMillis;
                spin=0;
            }
            localServerChannel = null;
            channelRead(ctx, msg);
        }
    }

}
