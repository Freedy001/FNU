package com.freedy;

import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.Skip;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.utils.EncryptUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @author Freedy
 * @date 2021/12/11 10:14
 */
@Data
@Slf4j
@InjectProperties(value = "encryption",exclude = "log")
public class EncryptProp {
    private Boolean enabled;
    private String aesKey;
    private Integer authenticationTime;
    @Skip
    private byte[] authenticationToken;

    @PostConstruct
    private void initToken() {
        if (!enabled) {
            aesKey = null;
            authenticationToken = new byte[32];
            return;
        }
        if (aesKey == null || authenticationTime == 0)
            throw new IllegalArgumentException("when you enable encrypt mode you must specify ? and ?", "aesKey", "authenticationTime must gt 1");
        String token = aesKey;
        for (int i = 0; i < authenticationTime; i++) {
            token = EncryptUtil.stringToMD5(token);
        }
        authenticationToken = token.getBytes(StandardCharsets.UTF_8);
        log.info("start encrypt mode,aesKey:{},authenticationTime:{}", aesKey, authenticationTime);
    }
}
