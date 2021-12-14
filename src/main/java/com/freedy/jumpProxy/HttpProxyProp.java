package com.freedy.jumpProxy;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import lombok.Data;

/**
 * @author Freedy
 * @date 2021/12/9 9:29
 */
@Data
@InjectProperties(value = "proxy.http",nonePutIfEmpty = true)
public class HttpProxyProp {
    private Boolean enabled;
    private Integer port = 1000;
    private Boolean jumpEndPoint = false;
}
