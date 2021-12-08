package com.freedy.manage;

import com.freedy.tinyFramework.RequestInterceptor;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @author Freedy
 * @date 2021/12/8 21:43
 */
@Part
@Slf4j
public class CrossAndLogInterceptor implements RequestInterceptor {

    @Override
    public boolean pre(FullHttpRequest httpRequest) {
        log.debug("receive one request url:{} method:{}",httpRequest.uri(),httpRequest.method());
        return true;
    }

    @Override
    public boolean post(FullHttpResponse httpResponse) {
        httpResponse.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        httpResponse.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "*");
        httpResponse.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "*");
        return true;
    }
}
