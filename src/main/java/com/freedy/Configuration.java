package com.freedy;

import lombok.*;

/**
 * @author Freedy
 * @date 2021/11/20 10:17
 */
@Data
public class Configuration {
    private String propertiesPath;
    private boolean startLocalIntranet=true;
    private boolean startRemoteIntranet=true;
    private boolean startHttpProxy=true;
    private boolean startReverseProxy=true;
    private boolean startLocalJumpHttpProxy=true;
    private boolean startRemoteJumpHttpProxy=true;
}
