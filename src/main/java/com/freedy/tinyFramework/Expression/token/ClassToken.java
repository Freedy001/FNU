package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Freedy
 * @date 2021/12/14 16:57
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ClassToken extends Token{
    protected boolean nullCheck;
    protected String propertyName;
    protected String methodName;
    protected String[] methodArgs;

    public ClassToken(String type, String value) {
        super(type, value);
    }

}
