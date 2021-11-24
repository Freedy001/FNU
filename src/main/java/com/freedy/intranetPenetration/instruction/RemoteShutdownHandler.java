package com.freedy.intranetPenetration.instruction;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Freedy
 * @date 2021/11/23 19:59
 */
public class RemoteShutdownHandler implements InstructionHandler {
    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        ctx.channel().parent().close();
        return false;
    }
}
