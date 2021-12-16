package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;

/**
 * @author Freedy
 * @date 2021/12/15 16:50
 */
@JSONType(ignores = {"numeric","strPattern","context","desiredType","notFlag","preSelfAddFlag","preSelfSubFlag","postSelfAddFlag","postSelfSubFlag"})
public class BasicVarToken extends Token {

    public BasicVarToken(String type, String value) {
        super(type, value);
    }


    @Override
    public Object doCalculate(Class<?> desiredType) {
        if (isType("str")) {
            return checkAndSelfOps(value);
        }
        if (isType("numeric")) { //int long
            if (desiredType.getName().matches("java\\.lang\\.Integer|int")) {
                return Integer.parseInt(value);
            }
            if (desiredType.getName().matches("java\\.lang\\.Long|long")) {
                return Long.parseLong(value);
            }
            boolean isLong = value.contains("l") || value.contains("L");
            return checkAndSelfOps(isLong ? Long.parseLong(value) :Integer.parseInt(value));
        }
        if (isType("bool")) {
            return checkAndSelfOps(notFlag != Boolean.parseBoolean(value));
        }
        throw new EvaluateException("illegal type ?", type).errStr(type);
    }


    @Override
    public void setNotFlag(boolean notFlag) {
        if (isAnyType("str", "numeric")) {
            throw new IllegalArgumentException("NOT OPS can not operate ? type", type);
        }
        super.setNotFlag(notFlag);
    }

    @Override
    public void setPreSelfAddFlag(boolean preSelfAddFlag) {
        if (isAnyType("str", "bool")) {
            throw new IllegalArgumentException("PRE SELF ADD (++?) can not operate ? type", value, type);
        }
        super.setPreSelfAddFlag(preSelfAddFlag);
    }

    @Override
    public void setPreSelfSubFlag(boolean preSelfSubFlag) {
        if (isAnyType("str", "bool")) {
            throw new IllegalArgumentException("Pre Self Sub (--?) can not operate ? type", value, type);
        }
        super.setPreSelfSubFlag(preSelfSubFlag);
    }

    @Override
    public void setPostSelfAddFlag(boolean postSelfAddFlag) {
        if (isAnyType("str", "bool")) {
            throw new IllegalArgumentException("Post Self Add (?++) can not operate ? type", value, type);
        }
        super.setPostSelfAddFlag(postSelfAddFlag);
    }

    @Override
    public void setPostSelfSubFlag(boolean postSelfSubFlag) {
        if (isAnyType("str", "bool")) {
            throw new IllegalArgumentException("Post Self Sub (?--) not operate ? type", value, type);
        }
        super.setPostSelfSubFlag(postSelfSubFlag);
    }


}
