package com.freedy;

import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.intranetPenetration.remote.IntranetServer;
import com.freedy.jumpProxy.local.LocalServer;
import com.freedy.jumpProxy.remote.RemoteServer;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.utils.ChannelUtils;
import com.freedy.utils.EncryptUtil;
import io.netty.channel.Channel;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Freedy
 * @date 2021/11/8 10:30
 */
public class Context {
    //本地服务端口号
    public final static int JUMP_LOCAL_PORT;
    //负载均衡
    public final static LoadBalance<Struct.IpAddress> JUMP_REMOTE_LB;
    //远程服务端口号
    public final static int JUMP_REMOTE_PORT;
    //AES 加密KEY
    public final static String AES_KEY;
    //多次MD5加密
    public final static byte[] AUTHENTICATION;
    //反向代理端口
    public final static int REVERSE_PROXY_PORT;
    //反向代理负载均衡器
    public final static LoadBalance<Struct.IpAddress> REVERSE_PROXY_LB;


    public final static int INTRANET_REMOTE_PORT;
    //内网穿透每组所缓存的管道数量
    public final static int INTRANET_CHANNEL_CACHE_SIZE;
    //内网穿透配置组
    public final static Struct.ConfigGroup[] INTRANET_GROUPS;

    public final static String PORT_CHANNEL_CACHE_LB_NAME;

    public final static int INTRANET_CHANNEL_RETRY_TIMES = 3;
    //读空闲次数
    public final static int INTRANET_READER_IDLE_TIMES = 5;
    //读超时时间
    public final static int INTRANET_READER_IDLE_TIME = 5;
    //连接失败次数
    public final static int INTRANET_MAX_BAD_CONNECT_TIMES = 90;

    public final static int HTTP_PROXY_PORT;

    //换行符
    public final static String LF = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\r\n" : "\n";
    //properties
    static String propertiesPath;

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
        if (properties.getProperty("jump.local.start","null").equals("true")) {
            JUMP_LOCAL_PORT = Integer.parseInt(properties.getProperty("jump.local.server.port"));
            JUMP_REMOTE_LB = LoadBalanceFactory.produceByAddressAndName(
                    properties.getProperty("jump.local.connect.address").split(","),
                    properties.getProperty("jump.local.loadBalancing.algorithm")
            );
            System.out.println("初始化跳跃Http(redirect server)代理");
            System.out.println("远程服务地址:" + properties.getProperty("jump.local.connect.address"));
            System.out.println("负载均衡算法:" + properties.getProperty("jump.local.loadBalancing.algorithm"));
        } else {
            JUMP_LOCAL_PORT = -1;
            JUMP_REMOTE_LB = null;
        }

        //启动远程代理服务
        if (properties.getProperty("jump.remote.start","null").equals("true")) {
            JUMP_REMOTE_PORT = Integer.parseInt(properties.getProperty("jump.remote.server.port"));
            System.out.println("初始化跳跃Http(remote proxy server)代理");
        } else {
            JUMP_REMOTE_PORT = -1;
        }

        //反向代理服务
        if (properties.getProperty("reverse.proxy.start","null").equals("true")) {
            REVERSE_PROXY_PORT = Integer.parseInt(properties.getProperty("reverse.proxy.port"));
            REVERSE_PROXY_LB = LoadBalanceFactory.produceByAddressAndName(
                    properties.getProperty("reverse.proxy.server.address").split(","),
                    properties.getProperty("reverse.proxy.loadBalancing.algorithm")
            );
            System.out.println("初始化反向代理服务器配置");
            System.out.println("反向负载地址:" + properties.getProperty("reverse.proxy.server.address"));
            System.out.println("负载均衡算法:" + properties.getProperty("reverse.proxy.loadBalancing.algorithm"));
        } else {
            REVERSE_PROXY_PORT = -1;
            REVERSE_PROXY_LB = null;
        }

        //内网穿透客户端
        if (properties.getProperty("intranet.local.start", "null").equals("true")) {
            INTRANET_CHANNEL_CACHE_SIZE =
                    Integer.parseInt(properties.getProperty("intranet.local.cache.channel.size", "30"));
            String[] a = properties.getProperty("intranet.local.group.localServerAddress", "null").split(",");
            String[] b = properties.getProperty("intranet.local.group.remoteIntranetAddress", "null").split(",");
            String[] c = properties.getProperty("intranet.local.group.remoteServerPort", "null").split(",");
            int length = a.length;
            if (length != b.length || length != c.length) {
                throw new IllegalArgumentException("The length values of the three values(intranet.local.group) are not equal");
            }
            if (a[0].equals("null")) throw new IllegalArgumentException("You should config intranet.local.group first");

            Struct.ConfigGroup[] groups = new Struct.ConfigGroup[length];

            for (int i = 0; i < length; i++) {
                Struct.ConfigGroup group = new Struct.ConfigGroup();
                Struct.IpAddress localServerAddress = ChannelUtils.parseAddress(a[i]);
                if (localServerAddress == null) throw new IllegalArgumentException("Illegal intranet.local.group.localServerAddress:" + a[i]);
                group.setLocalServerAddress(localServerAddress.address());
                group.setLocalServerPort(localServerAddress.port());

                Struct.IpAddress remoteIntranetAddress = ChannelUtils.parseAddress(b[i]);
                if (remoteIntranetAddress == null) throw new IllegalArgumentException("Illegal intranet.local.group.remoteIntranetAddress:" + b[i]);
                group.setRemoteAddress(remoteIntranetAddress.address());
                group.setRemotePort(remoteIntranetAddress.port());

                try {
                    group.setRemoteServerPort(Integer.parseInt(c[i]));
                }catch (NumberFormatException e){
                    throw new IllegalArgumentException("Illegal intranet.local.group.remoteServerPort:" + c[i]);
                }
                groups[i]=group;
            }
            INTRANET_GROUPS=groups;
            System.out.println("初始化内网穿透(client)配置");
            System.out.println("管道池中管道的数量:"+INTRANET_CHANNEL_CACHE_SIZE);
            System.out.println("分组配置");
            for (Struct.ConfigGroup group : groups) {
                System.out.println(group);
            }
        }else {
            INTRANET_GROUPS=null;
            INTRANET_CHANNEL_CACHE_SIZE=-1;
        }

        //内网穿透服务端
        if (properties.getProperty("intranet.remote.start", "null").equals("true")) {
            INTRANET_REMOTE_PORT=Integer.parseInt(properties.getProperty("intranet.remote.port","null"));
            PORT_CHANNEL_CACHE_LB_NAME=properties.getProperty("intranet.remote.channel.loadBalancing","Round Robin");
            System.out.println("初始化内网穿透(server)配置");
            System.out.println("管道负载均衡算法:"+PORT_CHANNEL_CACHE_LB_NAME);
        }else {
            INTRANET_REMOTE_PORT=-1;
            PORT_CHANNEL_CACHE_LB_NAME=null;
        }

        if (properties.getProperty("proxy.start","null").equals("true")) {
            HTTP_PROXY_PORT = Integer.parseInt(properties.getProperty("proxy.port"));
            System.out.println("初始化内HTTP代理服务器配置");
        } else {
            HTTP_PROXY_PORT = -1;
        }
    }

}
