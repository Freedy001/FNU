package com.freedy.tinyFramework.Expression.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Freedy
 * @date 2021/12/14 15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Token {
    protected String type;
    protected String value;

    public boolean isType(String type){
        return type.equals(this.type);
    }

    public boolean isValue(String val){
        return value.equals(val);
    }
}
