package com.freedy.intranetPenetration;

import io.netty.channel.Channel;

import java.util.function.Consumer;

/**
 * 同步Future
 * @author Freedy
 * @date 2021/11/21 0:22
 */
public class ParentChannelFuture {
    private Channel channel;
    private Consumer<Channel> consumer;

    public void setChannel(Channel channel) {
        if (consumer != null) {
            consumer.accept(channel);
        } else {
            this.channel = channel;
        }
    }


    public void addListener(Consumer<Channel> consumer) {
        if (channel != null) {
            consumer.accept(channel);
        } else {
            this.consumer = consumer;
        }
    }

}
