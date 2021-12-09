package com.freedy.intranetPenetration.instruction;

import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/23 19:50
 */
@Part
@Slf4j
public class HeartBeatRemoteErrorMsgHandler implements InstructionHandler {

    @Inject("sentinelDaemonThread")
    private Thread sentinelDaemonThread;

    @Override
    public Boolean apply(ChannelHandlerContext channelHandlerContext, String[] s) {
        log.warn("receive one channel is inactive,ready to check channel cache pool!");
        sentinelDaemonThread.interrupt();
        return true;
    }

}
