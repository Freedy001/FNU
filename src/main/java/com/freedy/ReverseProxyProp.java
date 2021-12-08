package com.freedy;

import com.freedy.tinyFramework.annotation.WebApplication;
import com.freedy.tinyFramework.beanFactory.Application;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/12/6 21:59
 */
@Slf4j
@WebApplication(port = 9090)
public class ReverseProxyProp {


    public static void main(String[] args) {
        Application application = new Application(ReverseProxyProp.class).run();
    }


}
