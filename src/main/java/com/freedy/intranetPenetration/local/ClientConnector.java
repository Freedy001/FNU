package com.freedy.intranetPenetration.local;

import com.freedy.AuthenticAndDecrypt;
import com.freedy.AuthenticAndEncrypt;
import com.freedy.EncryptProp;
import com.freedy.Struct;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/11/17 14:16
 */
@Slf4j
@Part
public class ClientConnector {

    @Inject
    private Bootstrap bootstrap;
    @Inject("remoteChannelMap")
    private Map<Struct.ConfigGroup, Set<Channel>> remoteChannelMap;
    @Inject
    private BeanFactory factory;
    @Inject
    private EncryptProp encryptProp;

    /**
     * 连接到远程服务
     *
     * @param group 配置消息
     * @return 是否连接成功
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean initConnection(Struct.ConfigGroup group) {
        try {
            Channel channel = bootstrap.connect(group.getRemoteAddress(), group.getRemotePort()).sync().channel();
            channel.pipeline().addLast(
                    new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new AuthenticAndEncrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken()),
                    new AuthenticAndDecrypt(encryptProp.getAesKey(),encryptProp.getAuthenticationToken(),Protocol::invokeHandler),
                    new ObjectEncoder(),
                    new ObjectDecoder(ClassResolvers.cacheDisabled(ClientConnector.class.getClassLoader())),
                    new ClientHandshake(group,factory)
            );

            log.debug("[client]发送配置消息[{}]", group);
            channel.writeAndFlush(group);

            Set<Channel> set = remoteChannelMap.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet());
            set.add(channel);
        } catch (Exception e) {
            log.error("[EXCEPTION]: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 连接到本地服务
     *
     * @param group         配置消息
     * @param remoteChannel 远程通讯的管道
     * @return 连接到本地服务后的管道
     */
    public Channel localServerConnect(Struct.ConfigGroup group, Channel remoteChannel) {
        try {
            Channel channel = bootstrap.connect(group.getLocalServerAddress(), group.getLocalServerPort()).sync().channel();
            channel.pipeline().addLast(
                    new ResponseForward(remoteChannel)
            );
            return channel;
        } catch (Exception e) {
            log.error("[EXCEPTION]: {}", e.getMessage());
        }
        return null;
    }


}
