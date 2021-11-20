package com.freedy.intranetPenetration;

import com.freedy.Struct;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.ReleaseUtil;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/11/19 17:00
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class ForwardTask implements Runnable {
    @Getter
    private Channel receiverChannel;
    private Object msg;

    @Override
    public void run() {
        if (receiverChannel.isActive()) {
            //打印日志
            receiverChannel.writeAndFlush(msg);
        }else {
            log.info("A message will be discarded,because the receiverChannel is not active!");
            ReleaseUtil.release(msg);
        }
    }

}
