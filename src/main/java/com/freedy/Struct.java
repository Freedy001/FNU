package com.freedy;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

import java.util.List;

/**
 * 一个结构体的集合
 *
 * @author Freedy
 * @date 2021/11/17 16:11
 */
public class Struct {

    /**
     * socket四元组
     */
    public record SocketQuad(
            String localAddress, int localPort,
            String remoteAddress, int remotePort
    ) {
    }

    /**
     * ip地址与端口号的结构体
     */
    public record IpAddress(String address, int port) {

        public boolean portEqual(IpAddress address) {
            return address.port() == port();
        }

        public boolean addressEqual(IpAddress address) {
            return address.address().equals(this.address);
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    /**
     * 缓存池中的channel状态
     */
    public record OccupyState(
            //是否被占用
            boolean occupied,
            //接收器的channel
            Channel receiverChannel,
            //需要被唤醒的队列
            List<Promise<Channel>> wakeupList
    ) {
    }


}
