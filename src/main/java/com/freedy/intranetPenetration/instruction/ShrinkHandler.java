package com.freedy.intranetPenetration.instruction;

import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.local.LocalProp;
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

    @Inject
    private LocalProp prop;

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        Channel channel = ctx.channel();
        int size = remoteChannelMap.get(ChannelUtils.getGroup(channel)).size();
        int shrinkSize = Integer.parseInt(s[0]);
        int minChannelCount = prop.getMinChannelCount();
        if (size - shrinkSize < minChannelCount) {
            shrinkSize = size - minChannelCount;
        }
        if (shrinkSize <= 0) {
            ChannelUtils.setCmd(channel, Protocol.SHRINK_RESP.param("refused").param(minChannelCount));
            return true;
        }
        ChannelUtils.setCmd(channel, Protocol.SHRINK_RESP.param("doShrink").param(shrinkSize));
        return true;
    }
}
