package com.freedy.tinyFramework.Expression.token;


/**
 * @author Freedy
 * @date 2021/12/14 15:50
 */

public class OpsToken extends Token {

    public OpsToken(String value) {
        super("operation", value);
    }

    public OpsToken(String type,String value) {
        super(type, value);
    }
}
