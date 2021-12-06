package com.freedy.manage.entity;

import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import lombok.Data;

import java.util.Date;

/**
 * @author Freedy
 * @date 2021/12/3 12:52
 */
@Data
@Part
public class B {
    private String name="B inject";
    private Date date=new Date();
    private int number=this.hashCode();
    @Inject
    private A a;

    @Override
    public String toString() {
        return "B{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", number=" + number +
                '}';
    }
}
