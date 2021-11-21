package com.freedy.loadBalancing;

import io.netty.channel.Channel;

/**
 * @author Freedy
 * @date 2021/11/16 20:18
 */
public class SourceIPHash<T> extends LoadBalance<T> {


    SourceIPHash() { }

    @Override
    public T supply() {
        if (attribute[0] instanceof Channel channel) {
            int hash = hash(channel.remoteAddress().toString());
            return (T)lbElement.get(hash % lbElement.size());
        } else {
            throw new IllegalArgumentException("Illegal attr! Need attr which type is io.netty.channel.Channel,please check your attr.");
        }
    }

    public static int hash(String socketAddress) {
        //255.255.255.255
        int[] ip = new int[4];
        int i = 0;
        for (String s : socketAddress.split(":")[0].split("\\.")) {
            ip[i++] = Integer.parseInt(s);
        }
        int a = ip[0] ^ ip[2];
        int b = ip[1] ^ ip[3];
        return a * b;
    }

}
