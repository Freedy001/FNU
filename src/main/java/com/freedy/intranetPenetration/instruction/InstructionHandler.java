package com.freedy.intranetPenetration.instruction;

import io.netty.channel.ChannelHandlerContext;

import java.util.function.BiFunction;

/**
 * @author Freedy
 * @date 2021/11/23 19:49
 */
public interface InstructionHandler extends BiFunction<ChannelHandlerContext, String[], Boolean> {

    @Override
    default Boolean apply(ChannelHandlerContext ctx, String[] params) {
        //do nothing here
        return true;
    }

    default Integer getIntParam(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
