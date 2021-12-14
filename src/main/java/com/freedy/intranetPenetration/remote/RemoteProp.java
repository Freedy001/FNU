package com.freedy.intranetPenetration.remote;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import lombok.Data;

/**
 * @author Freedy
 * @date 2021/12/9 18:03
 */
@Data
@InjectProperties(value = "intranet.remote",nonePutIfEmpty = true)
public class RemoteProp {
    private Boolean enabled;
    private Integer port;
    private String loadBalancing;
}
