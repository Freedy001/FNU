package com.freedy.manage.controller;

import com.freedy.Context;
import com.freedy.ServerStarter;
import com.freedy.log.LogRecorder;
import com.freedy.manage.Response;
import com.freedy.manage.entity.IntranetLocalEntity;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.mvc.Get;
import com.freedy.tinyFramework.annotation.mvc.REST;
import com.freedy.tinyFramework.beanFactory.BeanFactory;

import java.lang.reflect.Field;

import static com.freedy.tinyFramework.utils.StringUtils.convertEntityFieldToConstantField;

/**
 * @author Freedy
 * @date 2021/11/29 10:10
 */
@REST("root")
public class ManagerController {
    private final Class<Context> contextClass = Context.class;

    @Inject
    private BeanFactory beanFactory;

    @Inject
    private ServerStarter serverStarter;

    @Get
    public Response intranetLocal() {
        IntranetLocalEntity local = new IntranetLocalEntity();
        boolean isStart = beanFactory.containsBean("intranetLocalServer");

        local.setIsLocalStart(isStart);
        local.setStartTime(isStart ? serverStarter.getIntranetLocalStartTime() : null);
        for (Field field : IntranetLocalEntity.class.getDeclaredFields()) {
            try {
                String s = convertEntityFieldToConstantField(field.getName());
                Object o;
                o = contextClass.getDeclaredField(s).get(null);
                field.setAccessible(true);
                field.set(local, o);
            } catch (Exception ignored) {
            }
        }
        return Response.ok(local);
    }


    @Get
    public Response getLog(LogRecorder logRecorder) {
        return Response.ok(logRecorder.getLog());
    }



}
