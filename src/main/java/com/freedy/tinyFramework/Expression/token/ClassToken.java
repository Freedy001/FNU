package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Freedy
 * @date 2021/12/14 16:57
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ClassToken extends Token{
    private boolean nullCheck;
    private String propertyName;
    private String methodName;
    private String[] methodArgs;
}
