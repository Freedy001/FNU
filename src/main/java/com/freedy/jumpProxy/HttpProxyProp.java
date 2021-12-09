package com.freedy.jumpProxy;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NoneForce;
import lombok.Data;

/**
 * @author Freedy
 * @date 2021/12/9 9:29
 */
@Data
@NoneForce
@InjectProperties("proxy.http")
public class HttpProxyProp {
    private boolean enabled;
    private int port = 1000;
    private boolean jumpEndPoint = false;
}
