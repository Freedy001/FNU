package com.freedy.manage;

import com.freedy.log.LogRecorder;
import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import com.freedy.tinyFramework.annotation.beanContainer.Part;

import java.io.PrintStream;

/**
 * @author Freedy
 * @date 2021/12/5 0:12
 */
@Part(configure = true)
public class Config {

    @Bean
    public LogRecorder logRecorder(){
        LogRecorder logRecorder = new LogRecorder(System.out);
        System.setOut(new PrintStream(logRecorder));
        return logRecorder;
    }

    @Bean
    public Response response(){
        return Response.ok("HAHAHA");
    }

}
