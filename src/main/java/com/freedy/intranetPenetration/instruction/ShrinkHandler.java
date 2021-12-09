package com.freedy.intranetPenetration.instruction;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/11/24 10:57
 */
@Part
public class ShrinkHandler implements InstructionHandler {


    @Inject("remoteChannelMap")
    private Map<Struct.ConfigGroup, List<Channel>> remoteChannelMap;

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        Channel channel = ctx.channel();
        int size = remoteChannelMap.get(ChannelUtils.getGroup(channel)).size();
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
