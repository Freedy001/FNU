package com.freedy.utils;

import com.freedy.Struct;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/11/18 16:38
 */
public class ChannelUtils {

    private static final AttributeKey<Struct.SocketQuad> groupInfo = AttributeKey.valueOf("groupInfo");

    public static Struct.SocketQuad getGroup(Channel channel) {
        return channel.attr(groupInfo).get();
    }

    public static void setGroup(Channel channel, Struct.SocketQuad group) {
        channel.attr(groupInfo).set(group);
    }


    public static boolean sendString(Channel channel, String s) {
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.copiedBuffer(s, Charset.defaultCharset()));
            return true;
        }
        return false;
    }


    private final static Pattern pattern = Pattern.compile("((\\d{1,3}\\.){3}(\\d{1,3})):(.*?)[^\\d]");

    public static Struct.IpAddress parseAddress(SocketAddress address) {
        return parseAddress(address.toString());
    }

    public static Struct.IpAddress parseAddress(String address) {
        try {
            Matcher matcher = pattern.matcher(address + " ");
            if (!matcher.find()) return null;
            return new Struct.IpAddress(matcher.group(1), Integer.parseInt(matcher.group(4)));
        } catch (Exception e) {
            return null;
        }
    }


}
