package com.freedy.intranetPenetration;

import com.freedy.Context;
import com.freedy.Struct;
import com.freedy.intranetPenetration.instruction.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/11/18 16:31
 */
@Slf4j
public class Protocol {

    //握手指令
    public final static String ACK = "ACK!";
    public final static String CONNECTION_SUCCESS_MSG = "CONNECT ESTABLISH SUCCEED!";
    //客户端发送的指令,服务端处理的方法
    public final static Instruction HEARTBEAT_LOCAL_NORMAL_MSG =
            new Instruction("I AM ALIVE!", new HeartBeatLocalNormalMsgHandler());

    public final static Instruction HEARTBEAT_LOCAL_ERROR_MSG =
            new Instruction("LOCAL SERVER DOWN!", new HeartbeatLocalErrorMsgHandler());

    public final static Instruction EXPEND_RESP =
            new Instruction("EXPEND STATE!", new ExpendRespHandler());

    public final static Instruction REMOTE_SHUTDOWN =
            new Instruction("SHUTDOWN!", new RemoteShutdownHandler());

    public final static Instruction SHRINK_RESP =
            new Instruction("SHRINK STATE!", new ShrinkRespHandler());


    //服务端发送的指令,客户端处理的方法
    public final static Instruction HEARTBEAT_REMOTE_NORMAL_MSG =
            new Instruction("ROGER!", new InstructionHandler() {
            });

    //立即检查缓存
    public final static Instruction HEARTBEAT_REMOTE_ERROR_MSG =
            new Instruction("REFRESH CHANNEL!", new HeartBeatRemoteErrorMsgHandler());

    //扩容管道
    public final static Instruction EXPEND =
            new Instruction("EXPEND CHANNEL!", new ExpendHandler());

    public final static Instruction SHRINK =
            new Instruction("SHRINK CHANNEL!", new ShrinkHandler());


    private final static Map<String, Instruction> instructionMap = new HashMap<>();

    static {
        try {
            for (Field field : Protocol.class.getDeclaredFields()) {
                if (field.getType() == Instruction.class) {
                    Instruction o = (Instruction) field.get(null);
                    instructionMap.put(o.protocolName, o);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean invokeHandler(ChannelHandlerContext ctx, String protocolMsg) {
        Struct.ProtocolInfo info = getInfo(protocolMsg);
        Instruction instruction = instructionMap.get(info.name());
        if (instruction == null) {
            log.warn("illegal instruction: {}", protocolMsg);
            return false;
        }
        return instruction.handler.apply(ctx, info.parameter().split("!"));
    }

    public static Struct.ProtocolInfo getInfo(String msg) {
        String[] split = msg.trim().split("!", 2);
        return new Struct.ProtocolInfo(split[0] + "!", split.length == 2 ? split[1] : null);
    }

    public static void main(String[] args) throws IllegalAccessException {
        for (Field field : Context.class.getDeclaredFields()) {
            if (field.getType() == Instruction.class) {
                String s = "_" + field.getName().toLowerCase(Locale.ROOT);
                int lastIndex = 0;
                while (true) {
                    int index = s.indexOf("_");
                    if (index == -1) break;
                    s = s.substring(lastIndex, index) + s.substring(index + 1, index + 2).toUpperCase(Locale.ROOT) + s.substring(index + 2);
                }
                System.out.println(s + "Handler");
            }
        }
    }

    public static class Instruction {
        @Getter
        private final String protocolName;
        private final ThreadLocal<String> param = new ThreadLocal<>();
        private final InstructionHandler handler;

        private Instruction(String protocolName, InstructionHandler handler) {
            this.protocolName = protocolName;
            this.handler = handler;
        }

        public Instruction param(Object o) {
            if (o.toString().contains("!")) throw new IllegalArgumentException("param shouldn't contain character(!)");
            String s = param.get();
            if (s != null) {
                param.set(s + "!" + o);
            } else {
                param.set(String.valueOf(o));
            }
            return this;
        }

        public String getParam() {
            String s = param.get();
            param.remove();
            return s;
        }
    }
}
