package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Freedy
 * @date 2021/12/15 16:50
 */
@NoArgsConstructor
@JSONType(includes = {"type", "value"})
public final class BasicVarToken extends Token {

    public BasicVarToken(String type, String value) {
        super(type, value);
    }


    @Override
    public Object doCalculate(Class<?> desiredType) {
        if (isType("str")) {
            return checkAndSelfOps(ReflectionUtils.convertType(value, desiredType));
        }
        if (isType("numeric")) { //int long
            if (desiredType.getName().matches("java\\.lang\\.Integer|int")) {
                return selfOps(Double.valueOf(value).intValue());
            }
            if (desiredType.getName().matches("java\\.lang\\.Long|long")) {
                return selfOps(Double.valueOf(value).longValue());
            }
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            if (new BigDecimal(value).compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
                if (new BigDecimal(value).compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
                    throw new EvaluateException("? exceed the max of the Long ?", value, Long.MAX_VALUE);
                }
                return checkAndSelfOps(Long.parseLong(value));
            } else {
                return checkAndSelfOps(Integer.parseInt(value));
            }
        }
        if (isType("bool")) {
            return checkAndSelfOps(notFlag != Boolean.parseBoolean(value));
        }
        throw new EvaluateException("illegal type ?", type).errToken(this);
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
