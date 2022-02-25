package com.freedy;

import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * @author Freedy
 * @date 2021/11/8 10:30
 */
@Slf4j
public class Context {
    //连接本地服务器失败次数
    public final static int INTRANET_CHANNEL_RETRY_TIMES = 3;
    //读空闲次数
    public final static int INTRANET_READER_IDLE_TIMES = 3;
    //读超时时间
    public final static int INTRANET_READER_IDLE_TIME = 5;
    //连接失败次数
    public final static int INTRANET_MAX_BAD_CONNECT_TIMES = 90;
    //当服务的管道为0时，需要空闲多久关闭该服务
    public final static long INTRANET_SERVER_ZERO_CHANNEL_IDLE_TIME = 15 * 60 * 1_000_000_000L; //15min
    //指定最大长度
    public final static int CMD_LENGTH = 28;
    //换行符
    public final static String LF = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\r\n" : "\n";




    public static int TEST = 28;

    public static Object test(){
        System.out.println("hello");
        return "你好";
    }
}
