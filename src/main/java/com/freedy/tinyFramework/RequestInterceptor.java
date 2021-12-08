package com.freedy.tinyFramework;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author Freedy
 * @date 2021/12/2 15:03
 */
public interface RequestInterceptor {

    boolean pre(FullHttpRequest httpRequest);


    boolean post(FullHttpResponse httpResponse);
}
