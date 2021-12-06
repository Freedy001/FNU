package com.freedy.manage.entity;


import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import lombok.Data;

import java.util.Date;

/**
 * @author Freedy
 * @date 2021/11/29 17:32
 */
@Part
@Data
public class A {
    private String name = "A inject";
    private Date date = new Date();
    private int number = this.hashCode();
    @Inject
    public B b;

    @Override
    public String toString() {
        return "A{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", number=" + number +
                '}';
    }
}
