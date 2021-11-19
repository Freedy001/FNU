package com.freedy;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一个结构体的集合
 *
 * @author Freedy
 * @date 2021/11/17 16:11
 */
public class Struct {

    /**
     * 内网穿透配置组信息
     */
    public record ConfigGroup(
            //本地需要穿透的服务的ip与端口
            String localServerAddress, int localServerPort,
            //远程服务器的ip与端口
            String remoteAddress, int remotePort,
            //远程服务器提供服务的端口
            int remoteServerPort
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
            return address.address().equals(address());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IpAddress address) {
                return portEqual(address) && addressEqual(address);
            }
            return false;
        }
    }
}
