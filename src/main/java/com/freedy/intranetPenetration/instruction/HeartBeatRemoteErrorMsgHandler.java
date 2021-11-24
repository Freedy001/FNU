package com.freedy.intranetPenetration.instruction;

import com.freedy.intranetPenetration.local.ClientConnector;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/23 19:50
 */
@Slf4j
public class HeartBeatRemoteErrorMsgHandler implements InstructionHandler {

    @Override
    public Boolean apply(ChannelHandlerContext channelHandlerContext, String[] s) {
        log.warn("receive one channel is inactive,ready to check channel cache pool!");
        ClientConnector.sentinelDaemonThread.interrupt();
        return true;
    }

}
