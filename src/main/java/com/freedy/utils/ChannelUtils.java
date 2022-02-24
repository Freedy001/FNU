package com.freedy.utils;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/11/18 16:38
 */
@SuppressWarnings("UnusedReturnValue")
public class ChannelUtils {

    private static final AttributeKey<Struct.ConfigGroup> GROUP_INFO = AttributeKey.valueOf("groupInfo");
    private static final AttributeKey<NioEventLoopGroup> BOSS_EVENT = AttributeKey.valueOf("bossEvent");
    private static final AttributeKey<Boolean> IS_INIT = AttributeKey.valueOf("isInit");
    private static final AttributeKey<OccupyState> OCCUPIED = AttributeKey.valueOf("occupied");
    private static final AttributeKey<Boolean> DESTROY_STATE = AttributeKey.valueOf("destroyState");
    private static final AttributeKey<byte[]> CMD = AttributeKey.valueOf("cmd");
    private static final AttributeKey<Integer> FAIL_TIMES=AttributeKey.valueOf("failTimes");

    public static Struct.ConfigGroup getGroup(Channel channel) {
        return channel.attr(GROUP_INFO).get();
    }

    public static void setGroup(Channel channel, Struct.ConfigGroup group) {
        channel.attr(GROUP_INFO).set(group);
    }

    public static NioEventLoopGroup getBossEvent(Channel channel) {
        return channel.attr(BOSS_EVENT).get();
    }

    public static void setBossEvent(Channel channel, NioEventLoopGroup group) {
        channel.attr(BOSS_EVENT).set(group);
    }

    public static boolean isInit(Channel channel) {
        Boolean isInit = channel.attr(IS_INIT).get();
        return isInit != null && isInit;
    }

    public static void setInit(Channel channel, boolean state) {
        channel.attr(IS_INIT).set(state);
    }

    public static void setOccupy(Channel channel, OccupyState state) {
        channel.attr(OCCUPIED).set(state);
    }

    public static OccupyState getOccupy(Channel channel) {
        return channel.attr(OCCUPIED).get();
    }

    public static Integer getFileTimes(Channel channel) {
        return channel.attr(FAIL_TIMES).get();
    }

    public static void setFailTimes(Channel channel, Integer failTimes) {
        channel.attr(FAIL_TIMES).set(failTimes);
    }


    private final static byte spaceByte = " ".getBytes(StandardCharsets.UTF_8)[0];

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

    public static void setDestroyState(Channel channel, boolean state) {
        channel.attr(DESTROY_STATE).set(state);
    }

    public static boolean getDestroyState(Channel channel) {
        return Optional.ofNullable(channel.attr(DESTROY_STATE).get()).orElse(false);
    }

    public static boolean setCmdAndSendIfAbsent(Channel channel, Protocol.Instruction s) {
        boolean b = getCmd(channel) == null;
        if (b) {
            setCmd(channel, s);
        }
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER);
        return b;
    }

    public static void setCmdAndSend(Channel channel, Protocol.Instruction s) {
        setCmd(channel, s);
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    public static void setCmd(Channel channel, Protocol.Instruction cmd) {
        String msg = cmd.getProtocolName() + Optional.ofNullable(cmd.getParam()).orElse("");

        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        int cmdLength = Context.CMD_LENGTH;
        if (length > cmdLength) throw new IllegalArgumentException("cmd length must less than " + cmdLength);
        byte[] cmdBytes = new byte[cmdLength];
        Arrays.fill(cmdBytes, spaceByte);
        System.arraycopy(bytes, 0, cmdBytes, 0, length);
        channel.attr(ChannelUtils.CMD).set(cmdBytes);
    }


    public static byte[] getCmd(Channel channel) {
        return channel.attr(CMD).get();
    }

    public static void clearCmd(Channel channel) {
        channel.attr(CMD).set(null);
    }

    private final static Pattern pattern = Pattern.compile("(.*?):(.*?)[^\\d]");

    public static Struct.IpAddress parseAddress(SocketAddress address) {
        return parseAddress(address.toString());
    }

    public static Struct.IpAddress parseAddress(String address) {
        try {
            Matcher matcher = pattern.matcher(address + " ");
            if (!matcher.find()) return null;
            return new Struct.IpAddress(matcher.group(1), Integer.parseInt(matcher.group(2)));
        } catch (Exception e) {
            return null;
        }
    }

}
