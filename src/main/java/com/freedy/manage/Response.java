package com.freedy.manage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * @author Freedy
 * @date 2021/11/29 10:11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    private int code;
    private String msg;
    private Object data;

    public static Response ok(Object data) {
        return new Response(200, "ok", data);
    }

    public static Response err() {
        return err( "server error");
    }

    public static Response err(String msg) {
        return err(500, msg);
    }

    public static Response err(int code, String msg) {
        return new Response(code, msg, null);
    }

    public void okhaha(Object a,Object ...data){
        System.out.println("okhaha"+ Arrays.toString(data));
    }

}
