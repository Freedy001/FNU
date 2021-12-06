package com.freedy.manage.controller;

import com.freedy.Context;
import com.freedy.Start;
import com.freedy.log.LogRecorder;
import com.freedy.manage.Response;
import com.freedy.manage.entity.IntranetLocalEntity;
import com.freedy.tinyFramework.annotation.mvc.Get;
import com.freedy.tinyFramework.annotation.mvc.REST;

import java.lang.reflect.Field;

import static com.freedy.tinyFramework.utils.StringUtils.convertEntityFieldToConstantField;

/**
 * @author Freedy
 * @date 2021/11/29 10:10
 */
@REST("root")
public class ManagerController {
    private final Class<Context> contextClass = Context.class;

    @Get
    public Response intranetLocal(Start.InfoMap infoMap) {
        IntranetLocalEntity local = new IntranetLocalEntity();
        Start.StartInfo channelInfo = infoMap.get("INTRANET_LOCAL_SERVER");
        local.setIsLocalStart(channelInfo!=null);
        local.setStartTime(channelInfo!=null?channelInfo.startTime():-1);
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
