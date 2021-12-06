package com.freedy;

import com.freedy.intranetPenetration.local.ClientConnector;
import com.freedy.intranetPenetration.remote.IntranetServer;
import com.freedy.jumpProxy.local.LocalServer;
import com.freedy.jumpProxy.remote.RemoteServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;
import java.util.HashMap;


/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
@Slf4j
public class Start {


    public record StartInfo(Channel channel, long startTime) {
    }

    public static class InfoMap extends HashMap<String, StartInfo> {
        public StartInfo put(String key, Channel value) {
            return super.put(key, new StartInfo(value, System.currentTimeMillis()));
        }
    }


    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) throws Exception {

        InfoMap nameChannelMap = new InfoMap();

        if (Context.JUMP_LOCAL_PORT != -1) {
            catchPortInUse(() -> {
                Channel channel = LocalServer.start(Context.JUMP_LOCAL_PORT, Context.JUMP_REMOTE_LB, false);
                nameChannelMap.put("JUMP_LOCAL_SERVER", channel);
            });
        }

        if (Context.JUMP_REMOTE_PORT != -1) {
            catchPortInUse(() -> {
                Channel channel = RemoteServer.start(Context.JUMP_REMOTE_PORT, false);
                nameChannelMap.put("JUMP_REMOTE_SERVER", channel);
            });
        }

        if (Context.REVERSE_PROXY_PORT != -1) {
            catchPortInUse(() -> {
                Channel channel = LocalServer.start(Context.REVERSE_PROXY_PORT, Context.REVERSE_PROXY_LB, true);
                nameChannelMap.put("REVERSE_PROXY_SERVER", channel);
            });
        }

        if (Context.HTTP_PROXY_PORT != -1) {
            catchPortInUse(() -> {
                Channel channel = RemoteServer.start(Context.HTTP_PROXY_PORT, true);
                nameChannelMap.put("HTTP_PROXY_SERVER", channel);
            });
        }

        if (Context.INTRANET_CHANNEL_CACHE_MIN_SIZE != -1) {
            ClientConnector.start().addListener(channel -> nameChannelMap.put("INTRANET_LOCAL_SERVER", channel));
        }

        if (Context.INTRANET_REMOTE_PORT != -1) {
            catchPortInUse(() -> {
                Channel channel = IntranetServer.start();
                nameChannelMap.put("INTRANET_REMOTE_SERVER", channel);
            });
        }

//        RestProcessor handler =new RestProcessor();
//
//        handler.registerInnerObj(nameChannelMap);
//        if (Context.MANAGE_PORT != -1) {
//            catchPortInUse(() -> ManagerServer.start(handler));
//        }
//        Application application = new Application(Start.class).run();
//        for (String beanName : application.getAllBeanNames()) {
//            System.out.println(beanName);
//            Object bean = application.getBean(beanName);
//            System.out.println(bean);
//            log.info("next");
//        }
//        LockSupport.park();

    }



    @SuppressWarnings("ConstantConditions")
    public static void catchPortInUse(Runnable s) {
        try {
            s.run();
        } catch (Exception e) {
            if (e instanceof BindException bindException) {
                log.error(bindException.getMessage());
                log.error("port[{}] is used", Context.JUMP_LOCAL_PORT);
            } else
                e.printStackTrace();
        }
    }

}
