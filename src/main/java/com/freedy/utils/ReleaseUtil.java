package com.freedy.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCounted;

/**
 * @author Freedy
 * @date 2021/11/9 10:28
 */
public class ReleaseUtil {


    public static void release(Object msg) {
        if (msg == null) return;
        if (msg instanceof ReferenceCounted m) {
            if (m.refCnt() > 0)
                m.release();
        }
    }


    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
