package com.freedy.intranetPenetration.instruction;

import com.freedy.Context;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Freedy
 * @date 2021/11/24 10:57
 */
public class ShrinkHandler implements InstructionHandler {


    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        Channel channel = ctx.channel();
        int size = ClientConnector.remoteChannelMap.get(ChannelUtils.getGroup(channel)).size();
        int shrinkSize = Integer.parseInt(s[0]);
        if (size - shrinkSize < Context.INTRANET_CHANNEL_CACHE_MIN_SIZE) {
            shrinkSize = size - Context.INTRANET_CHANNEL_CACHE_MIN_SIZE;
        }
        if (shrinkSize <= 0) {
            ChannelUtils.setCmd(channel, Protocol.SHRINK_RESP.param("refused").param(Context.INTRANET_CHANNEL_CACHE_MIN_SIZE));
            return true;
        }
        ChannelUtils.setCmd(channel, Protocol.SHRINK_RESP.param("doShrink").param(shrinkSize));
        return true;
    }
}
