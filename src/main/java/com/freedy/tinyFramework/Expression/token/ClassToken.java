package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.StringJoiner;

/**
 * @author Freedy
 * @date 2021/12/14 16:57
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ClassToken extends Token implements Assignable{
    protected boolean nullCheck;
    protected String propertyName;
    protected String methodName;
    protected String[] methodArgs;


    public ClassToken(String type, String value) {
        super(type, value);
    }


    public String getMethodStr(){
        if (StringUtils.isEmpty(methodName)) return null;
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (String methodArg : methodArgs) {
            joiner.add(methodArg);
        }
        return methodName+ joiner;
    }

}
