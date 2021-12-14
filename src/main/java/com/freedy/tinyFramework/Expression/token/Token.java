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
public class Token {
    private String type;
    private String value;

    public boolean isType(String type){
        return type.equals(this.type);
    }
}
