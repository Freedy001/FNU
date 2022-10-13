package com.freedy;

import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NonStrict;
import lombok.Data;

/**
 * @author Freedy
 * @date 2022/10/13 23:13
 */
@Data
@NonStrict
@InjectProperties(value = "static.server", nonePutIfEmpty = true)
public class StaticServerProp {
    private Boolean enable;
    private String path="";
    private int port=3212;
}
