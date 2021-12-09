package com.freedy.intranetPenetration.instruction;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

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
    private Map<Struct.ConfigGroup, List<Channel>> remoteChannelMap;

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] param) {
        log.warn("receive expand cmd,start to expand channel cache size");
        Channel channel = ctx.channel();
        Struct.ConfigGroup group = ChannelUtils.getGroup(channel);
        int expandSize = Integer.parseInt(param[0]);
        if (expandSize > Context.INTRANET_CHANNEL_CACHE_MAX_SIZE) {
            expandSize = Context.INTRANET_CHANNEL_CACHE_MAX_SIZE - remoteChannelMap.get(group).size();
        }
        if (expandSize == 0) {
            ChannelUtils.setCmdAndSend(channel, Protocol.EXPEND_RESP.param("refuse").param(Context.INTRANET_CHANNEL_CACHE_MAX_SIZE));
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
