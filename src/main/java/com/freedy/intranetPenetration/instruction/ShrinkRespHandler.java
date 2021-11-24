package com.freedy.intranetPenetration.instruction;

import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.remote.ChanelWarehouse;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Freedy
 * @date 2021/11/24 12:50
 */
@Slf4j
public class ShrinkRespHandler implements InstructionHandler {


    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] params) {
        if (params[0].equals("refused")) {
            log.warn("client refused to shrink channel,because the channel size has dropped to freezing point({})!", params[1]);
            //缩容失败 锁定缩容检测
            ChannelUtils.getOccupy(ctx.channel()).lockShrinkCheck();
        } else {
            Integer shrinkCount = getIntParam(params[1]);
            if (shrinkCount == null) return true;
            List<Channel> channelList = ChanelWarehouse.PORT_CHANNEL_CACHE.get(ChannelUtils.getGroup(ctx.channel()).getRemoteServerPort()).getAllSafely();
            int actual = 0;
            for (Channel channel : channelList) {
                OccupyState occupy = ChannelUtils.getOccupy(channel);
                if (!occupy.isOccupy()) {
                    ChannelUtils.setDestroyState(channel, true);
                    ReleaseUtil.closeOnFlush(channel);
                    actual++;
                    if (actual >= shrinkCount) break;
                }
            }
            log.warn("shrink success! client expect shrink size:{},actual shrink size:{}", shrinkCount, actual);
        }
        //无论是否成功都，解锁扩容检测
        ChannelUtils.getOccupy(ctx.channel()).unlockExpandCheck();
        return true;
    }
}
