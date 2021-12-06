package com.freedy.tinyFramework;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/12/4 14:06
 */
@RequiredArgsConstructor
public class WaitingNode {
    @Getter
    private final Map<Field,String> fieldList=new HashMap<>();
    final Object proxy;

    public WaitingNode put(Field field,String name){
        fieldList.put(field,name);
        return this;
    }

}
