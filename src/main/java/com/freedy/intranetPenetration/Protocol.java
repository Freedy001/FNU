package com.freedy.intranetPenetration;

import com.freedy.Struct;
import com.freedy.intranetPenetration.instruction.*;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/11/18 16:31
 */
@Slf4j
@Part
public class Protocol {

    //握手指令
    public final static String ACK = "ACK!";
    public final static String CONNECTION_SUCCESS_MSG = "CONNECT ESTABLISH SUCCEED!";
    //客户端发送的指令,服务端处理的方法
    public static Instruction HEARTBEAT_LOCAL_NORMAL_MSG;
    public static Instruction HEARTBEAT_LOCAL_ERROR_MSG;
    public static Instruction EXPEND_RESP;
    public static Instruction REMOTE_SHUTDOWN;
    public static Instruction SHRINK_RESP;


    //服务端发送的指令,客户端处理的方法
    public static Instruction HEARTBEAT_REMOTE_NORMAL_MSG;
    //立即检查缓存
    public static Instruction HEARTBEAT_REMOTE_ERROR_MSG;
    //扩容管道
    public static Instruction EXPEND;
    public static Instruction SHRINK;


    private final static Map<String, Instruction> instructionMap = new HashMap<>();

    public Protocol(BeanFactory beanFactory) throws Exception{
        HEARTBEAT_LOCAL_NORMAL_MSG =
                new Instruction("I AM ALIVE!", beanFactory.getBean(HeartBeatLocalNormalMsgHandler.class));

        HEARTBEAT_LOCAL_ERROR_MSG =
                new Instruction("LOCAL SERVER DOWN!", beanFactory.getBean(HeartbeatLocalErrorMsgHandler.class));

        EXPEND_RESP =
                new Instruction("EXPEND STATE!", beanFactory.getBean(ExpendRespHandler.class));

        REMOTE_SHUTDOWN =
                new Instruction("SHUTDOWN!", beanFactory.getBean(RemoteShutdownHandler.class));

        SHRINK_RESP =
                new Instruction("SHRINK STATE!", beanFactory.getBean(ShrinkRespHandler.class));


        //服务端发送的指令,客户端处理的方法
        HEARTBEAT_REMOTE_NORMAL_MSG =
                new Instruction("ROGER!", new InstructionHandler() {
                });

        //立即检查缓存
        HEARTBEAT_REMOTE_ERROR_MSG =
                new Instruction("REFRESH CHANNEL!", beanFactory.getBean(HeartBeatRemoteErrorMsgHandler.class));

        //扩容管道
        EXPEND = new Instruction("EXPEND CHANNEL!", beanFactory.getBean(ExpendHandler.class));

        SHRINK = new Instruction("SHRINK CHANNEL!", beanFactory.getBean(ShrinkHandler.class));
        for (Field field : Protocol.class.getDeclaredFields()) {
            Object o = field.get(null);
            if (o instanceof Instruction instruction){
                instructionMap.put(instruction.protocolName, instruction);
            }
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
