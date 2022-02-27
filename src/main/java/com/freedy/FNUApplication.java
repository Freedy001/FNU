package com.freedy;

import com.freedy.tinyFramework.beanFactory.Application;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
@Slf4j
public class FNUApplication {


    public static void main(String[] args) {
        new Application(FNUApplication.class).run();
    }

}
