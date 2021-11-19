package com.freedy.utils;

import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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

    private static final AttributeKey<Struct.ConfigGroup> GROUP_INFO = AttributeKey.valueOf("groupInfo");

    public static Struct.ConfigGroup getGroup(Channel channel) {
        return channel.attr(GROUP_INFO).get();
    }

    public static void setGroup(Channel channel, Struct.ConfigGroup group) {
        channel.attr(GROUP_INFO).set(group);
    }

    private static final AttributeKey<Boolean> IS_INIT = AttributeKey.valueOf("isInit");

    public static boolean isInit(Channel channel) {
        Boolean isInit = channel.attr(IS_INIT).get();
        return isInit != null && isInit;
    }

    public static void setInit(Channel channel, boolean state) {
        channel.attr(IS_INIT).set(state);
    }

    private static final AttributeKey<OccupyState> occupied =AttributeKey.valueOf("occupied");

    public static void setOccupy(Channel channel,OccupyState state){
        channel.attr(occupied).set(state);
    }

    public static OccupyState getOccupy(Channel channel){
        return channel.attr(occupied).get();
    }


    public static boolean sendString(Channel channel, String s) {
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.copiedBuffer(s, Charset.defaultCharset()));
            return true;
        }
        return false;
    }

    public static boolean sendString(ChannelHandlerContext ctx, String s) {
        return sendString(ctx.channel(), s);
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
