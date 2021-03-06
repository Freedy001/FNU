package com.freedy.tinyFramework.exception;

import com.freedy.tinyFramework.utils.PlaceholderParser;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

/**
 * @author Freedy
 * @date 2021/12/4 11:05
 */
public class BeanException extends RuntimeException {

    public BeanException(Throwable cause) {
        super(cause);
    }

    public BeanException(String msg) {
        super(msg);
    }

    @SneakyThrows
    public BeanException(String msg, Object... placeholder) {
        Class<Throwable> aClass = Throwable.class;
        //设置cause
        for (int i = 0; i < placeholder.length; i++) {
            Object o = placeholder[i];
            if (o instanceof Throwable e) {
                Field cause = aClass.getDeclaredField("cause");
                cause.setAccessible(true);
                cause.set(this, o);
                placeholder[i] = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
                break;
            }
        }

        //设置msg
        Field exceptionMsg;
        exceptionMsg = aClass.getDeclaredField("detailMessage");
        exceptionMsg.setAccessible(true);
        exceptionMsg.set(this, new PlaceholderParser(msg, placeholder)
                .configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN)
                .registerNoneHighLightClass(Throwable.class)
                .ifEmptyFillWith("")
                .toString());
    }
}
