package com.freedy;

import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.local.LocalServer;
import com.freedy.remote.RemoteServer;
import io.netty.channel.Channel;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

/**
 * @author Freedy
 * @date 2021/11/8 10:30
 */
public class Context {
    //本地服务端口号
    public final static int LOCAL_PORT;
    //负载均衡
    public final static LoadBalance<Struct.IpAddress> REMOTE_LB;
    //远程服务端口号
    public final static int REMOTE_PORT;
    //AES 加密KEY
    public final static String AES_KEY;
    //多次MD5加密
    public final static byte[] AUTHENTICATION;
    //反向代理端口
    public final static int PROXY_PORT;
    //反向代理负载均衡器
    public final static LoadBalance<Struct.IpAddress> PROXY_LB;


    public final static int INTRANET_REMOTE_PORT=8888;
    //内网穿透每组所缓存的管道数量
    public final static int INTRANET_CHANNEL_CACHE_SIZE = 30;
    //内网穿透配置组
    public final static Struct.ConfigGroup[] INTRANET_GROUPS = {new Struct.ConfigGroup(
            "127.0.0.1", 4567, 
            "127.0.0.1", 8888,
            9090
    )};

    public final static String PORT_CHANNEL_CACHE_LB_NAME = "Round Robin";
    public final static int INTRANET_CHANNEL_RETRY_TIME = 3;
    //读空闲次数
    public final static int INTRANET_READER_IDLE_TIMES = 5;
    //读超时时间
    public final static int INTRANET_READER_IDLE_TIME = 5;
    //连接失败次数
    public final static int INTRANET_MAX_BAD_CONNECT_TIMES=90;

    //换行符
    public final static String LF = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\r\n" : "\n";
    //properties
    private static String propertiesPath;

    static {
        Properties properties = new Properties();

        try {
            properties.load(propertiesPath != null ? new FileInputStream(propertiesPath) :
                    Context.class.getClassLoader().getResourceAsStream("conf.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //启动加密
        if (properties.getProperty("encryption.start","null").equals("true")) {
            AES_KEY = properties.getProperty("encryption.aes.key");
            int times = Integer.parseInt(properties.getProperty("encryption.authenticationTime"));
            String authStr = AES_KEY;
            for (int i = 0; i < times; i++) {
                authStr = EncryptUtil.stringToMD5(authStr);
            }
            AUTHENTICATION = authStr.getBytes(StandardCharsets.UTF_8);
            System.out.println("AES对称加密密钥:" + AES_KEY);
            System.out.println("认证加密次数:" + times);
        } else {
            AES_KEY = null;
            AUTHENTICATION = null;
        }

        //启动本地转发服务
        if (properties.getProperty("local.start","null").equals("true")) {
            LOCAL_PORT = Integer.parseInt(properties.getProperty("local.server.port"));
            REMOTE_LB = LoadBalanceFactory.produceByAddressAndName(
                    properties.getProperty("local.connect.address").split(","),
                    properties.getProperty("local.loadBalancing.algorithm")
            );
            System.out.println("远程服务地址:" + properties.getProperty("local.connect.address"));
            System.out.println("负载均衡算法:" + properties.getProperty("local.loadBalancing.algorithm"));
        } else {
            LOCAL_PORT = -1;
            REMOTE_LB = null;
        }

        //启动远程代理服务
        if (properties.getProperty("remote.start","null").equals("true")) {
            REMOTE_PORT = Integer.parseInt(properties.getProperty("remote.server.port"));
        } else {
            REMOTE_PORT = -1;
        }

        //反向代理服务
        if (properties.getProperty("proxy.start","null").equals("true")) {
            PROXY_PORT = Integer.parseInt(properties.getProperty("proxy.port"));
            PROXY_LB = LoadBalanceFactory.produceByAddressAndName(
                    properties.getProperty("proxy.server.address").split(","),
                    properties.getProperty("proxy.loadBalancing.algorithm")
            );
            System.out.println("反向负载地址:" + properties.getProperty("proxy.server.address"));
            System.out.println("负载均衡算法:" + properties.getProperty("proxy.loadBalancing.algorithm"));
        } else {
            PROXY_PORT = -1;
            PROXY_LB = null;
        }

    }

    public static void main(String[] args) throws Exception {

        ArrayList<Channel> channelList = new ArrayList<>();
        if (LOCAL_PORT != -1) {
            Channel channel = LocalServer.start(LOCAL_PORT, REMOTE_LB, false);
            channelList.add(channel);
        }
        if (REMOTE_PORT != -1) {
            Channel channel = RemoteServer.start();
            channelList.add(channel);
        }
        if (PROXY_PORT != -1) {
            Channel channel = LocalServer.start(PROXY_PORT, PROXY_LB, true);
            channelList.add(channel);
        }

        channelList.forEach(channel -> {
            try {
                channel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}
