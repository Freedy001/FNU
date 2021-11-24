package com.freedy.intranetPenetration.instruction;

import com.freedy.errorProcessor.ErrorHandler;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Freedy
 * @date 2021/11/23 19:58
 */
public class HeartbeatLocalErrorMsgHandler implements InstructionHandler {
    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        ErrorHandler.handle(ChannelUtils.getOccupy(ctx.channel()).getReceiverChannel(), null);
        return false;
    }
}
