package com.freedy.intranetPenetration.remote;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NoneForce;
import lombok.Data;

/**
 * @author Freedy
 * @date 2021/12/9 18:03
 */
@Data
@NoneForce
@InjectProperties("intranet.remote")
public class RemoteProp {
    private boolean enabled;
    private int port;
    private String loadBalancing;
}
