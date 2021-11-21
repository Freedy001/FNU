package com.freedy.intranetPenetration;

import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/19 17:00
 */
@Slf4j
public record ForwardTask(Channel receiverChannel, Object msg) {

    public void execute(Channel intranetChannel) {
        if (intranetChannel.isActive()) {
            intranetChannel.writeAndFlush(msg);
        } else {
            log.info("A message will be discarded,because the receiverChannel is not active!");
            ReleaseUtil.release(msg);
        }
    }

}
