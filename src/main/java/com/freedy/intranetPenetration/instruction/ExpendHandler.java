package com.freedy.intranetPenetration.instruction;

import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.intranetPenetration.local.LocalProp;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * @author Freedy
 * @date 2021/11/23 19:58
 */
@Slf4j
@Part
public class ExpendHandler implements InstructionHandler {

    @Inject
    private ClientConnector clientConnector;

    @Inject("remoteChannelMap")
    private Map<Struct.ConfigGroup, Set<Channel>> remoteChannelMap;

    @Inject
    private LocalProp prop;

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] param) {
        log.warn("receive expand cmd,start to expand channel cache size");
        Channel channel = ctx.channel();
        Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
        int expandSize = Integer.parseInt(param[0]);

        int maxChannelCount = prop.getMaxChannelCount();
        int maxExpandSize = maxChannelCount - remoteChannelMap.get(group).size();
        if (expandSize > maxExpandSize) {
            expandSize = maxExpandSize;
        }
        if (expandSize <= 0) {
            ChannelUtils.setCmdAndSend(channel, Protocol.EXPEND_RESP.param("refuse").param(maxChannelCount));
            return true;
        }
        int successSize = 0;
        for (int i = 0; i < expandSize; i++) {
            if (clientConnector.initConnection(group))
                successSize++;
        }
        log.info("expand {} channel for remote server", successSize);

        ChannelUtils.setCmdAndSend(channel, Protocol.EXPEND_RESP.param("success").param(successSize));
        return true;
    }


}
