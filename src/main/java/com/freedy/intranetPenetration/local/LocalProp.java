package com.freedy.intranetPenetration.local;

import com.freedy.Struct;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NoneForce;
import com.freedy.tinyFramework.annotation.prop.Skip;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.utils.ChannelUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/9 15:42
 */
@NoneForce
@Data
@InjectProperties("intranet.local")
public class LocalProp {
    private boolean enabled;
    private int minChannelCount;
    private int maxChannelCount;
    private List<Config> group;
    @Skip
    private List<Struct.ConfigGroup> configGroupList=new ArrayList<>();

    @PostConstruct
    public void init(){
        if (!enabled) return;
        for (Config config : group) {
            Struct.ConfigGroup group = new Struct.ConfigGroup();
            Struct.IpAddress localServerAddress = ChannelUtils.parseAddress(config.getLocalServerAddress());
            Struct.IpAddress remoteServerAddress = ChannelUtils.parseAddress(config.getRemoteIntranetAddress());
            if (localServerAddress==null){
                throw new IllegalArgumentException("Illegal IpAddress:?",config.getLocalServerAddress());
            }
            if (remoteServerAddress==null){
                throw new IllegalArgumentException("Illegal IpAddress:?",config.getRemoteIntranetAddress());
            }
            group.setLocalServerAddress(localServerAddress.address());
            group.setLocalServerPort(localServerAddress.port());
            group.setRemoteAddress(remoteServerAddress.address());
            group.setRemotePort(remoteServerAddress.port());

            group.setRemoteServerPort(config.getRemoteServerPort());
            configGroupList.add(group);
        }
    }

    @Data
    public static class Config{
        private String localServerAddress;
        private String remoteIntranetAddress;
        private int remoteServerPort;
    }
}
