package com.freedy;

import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.intranetPenetration.remote.IntranetServer;
import com.freedy.jumpProxy.local.LocalServer;
import com.freedy.jumpProxy.remote.RemoteServer;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import static com.freedy.Context.*;

/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
public class Start {
    public static void main(String[] args) throws Exception {
        Configuration configuration = new CMDParamParser(args).parse();
        propertiesPath = configuration.getPropertiesPath();

        Map<String, Channel> nameChannelMap=new HashMap<>();

        if (configuration.isStartLocalJumpHttpProxy()&&JUMP_LOCAL_PORT != -1) {
            Channel channel = LocalServer.start(JUMP_LOCAL_PORT, JUMP_REMOTE_LB, false);
            nameChannelMap.put("JUMP_LOCAL_SERVER",channel);
        }
        if (configuration.isStartRemoteJumpHttpProxy()&&JUMP_REMOTE_PORT != -1) {
            Channel channel = RemoteServer.start(JUMP_REMOTE_PORT, false);
            nameChannelMap.put("JUMP_REMOTE_SERVER",channel);
        }
        if (configuration.isStartReverseProxy()&&REVERSE_PROXY_PORT != -1) {
            Channel channel = LocalServer.start(REVERSE_PROXY_PORT, REVERSE_PROXY_LB, true);
            nameChannelMap.put("REVERSE_PROXY_SERVER",channel);
        }
        if (configuration.isStartHttpProxy() && HTTP_PROXY_PORT != -1) {
            Channel channel = RemoteServer.start(HTTP_PROXY_PORT, true);
            nameChannelMap.put("HTTP_PROXY_SERVER", channel);
        }
        if (configuration.isStartLocalIntranet() && INTRANET_CHANNEL_CACHE_MIN_SIZE != -1) {
            ClientConnector.start().addListener(channel -> nameChannelMap.put("INTRANET_LOCAL_SERVER", channel));
        }
        if (configuration.isStartRemoteIntranet() && INTRANET_REMOTE_PORT != -1) {
            Channel channel = IntranetServer.start();
            nameChannelMap.put("INTRANET_REMOTE_SERVER", channel);
        }

        LockSupport.park();
    }

}
