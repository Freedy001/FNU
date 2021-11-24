package com.freedy;

import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.intranetPenetration.remote.IntranetServer;
import com.freedy.jumpProxy.local.LocalServer;
import com.freedy.jumpProxy.remote.RemoteServer;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;



/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
public class Start {
    public static void main(String[] args) throws Exception {
        Map<String, Channel> nameChannelMap = new HashMap<>();

        if (Context.JUMP_LOCAL_PORT != -1) {
            Channel channel = LocalServer.start(Context.JUMP_LOCAL_PORT, Context.JUMP_REMOTE_LB, false);
            nameChannelMap.put("JUMP_LOCAL_SERVER", channel);
        }
        if (Context.JUMP_REMOTE_PORT != -1) {
            Channel channel = RemoteServer.start(Context.JUMP_REMOTE_PORT, false);
            nameChannelMap.put("JUMP_REMOTE_SERVER", channel);
        }
        if (Context.REVERSE_PROXY_PORT != -1) {
            Channel channel = LocalServer.start(Context.REVERSE_PROXY_PORT, Context.REVERSE_PROXY_LB, true);
            nameChannelMap.put("REVERSE_PROXY_SERVER", channel);
        }
        if (Context.HTTP_PROXY_PORT != -1) {
            Channel channel = RemoteServer.start(Context.HTTP_PROXY_PORT, true);
            nameChannelMap.put("HTTP_PROXY_SERVER", channel);
        }
        if (Context.INTRANET_CHANNEL_CACHE_MIN_SIZE != -1) {
            ClientConnector.start().addListener(channel -> nameChannelMap.put("INTRANET_LOCAL_SERVER", channel));
        }
        if (Context.INTRANET_REMOTE_PORT != -1) {
            Channel channel = IntranetServer.start();
            nameChannelMap.put("INTRANET_REMOTE_SERVER", channel);
        }

        LockSupport.park();
    }

}
