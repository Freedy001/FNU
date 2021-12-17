package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.Tokenizer;
import com.freedy.tinyFramework.exception.BeanException;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JSONType(includes = {"type","value"})
public class StaticToken extends ClassToken {
    private String opsClass;
    private final Pattern strPattern = Pattern.compile("^'(.*?)'$");
    private final Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");

    public StaticToken(String value) {
        super("static", value);
    }

    @Override
    public Object doCalculate(Class<?> desiredType) {
        try {
            if (StringUtils.isEmpty(opsClass)) {
                throw new EvaluateException("opsClass is null");
            }
            Class<?> staticClass = Class.forName(opsClass);
            if (StringUtils.hasText(propertyName)) {
                Field field = ReflectionUtils.getFieldRecursion(staticClass, propertyName);
                if (field==null){
                    throw new EvaluateException("NoSuch Field ?",propertyName).errStr(propertyName);
                }
                field.setAccessible(true);
                return checkAndSelfOps(field.get(null));
            }
            if (StringUtils.hasText(methodName)) {
                List<Object> args = new ArrayList<>();
                try {
                    for (String methodArg : methodArgs) {
                        Matcher matcher = strPattern.matcher(methodArg);
                        if (matcher.find()) {
                            args.add(matcher.group(1));
                            continue;
                        }
                        matcher = numeric.matcher(methodArg);
                        if (matcher.matches()) {
                            args.add(methodArg.matches(".*?[lL]$") ? Long.parseLong(methodArg) : Integer.parseInt(methodArg));
                            continue;
                        }
                        TokenStream stream = Tokenizer.getTokenStream(methodArg);
                        Expression expression = new Expression(stream);
                        args.add(expression.getValue(context));
                    }
                } catch (Exception e) {
                    StringJoiner joiner = new StringJoiner(",","(",")");
                    for (String a : methodArgs) {
                        joiner.add(a);
                    }
                    throw new EvaluateException("get method args failed,because ?", e).errStr(joiner.toString());
                }
                try {
                    Method method = staticClass.getMethod(methodName, args.stream().map(Object::getClass).toArray(Class[]::new));
                    method.setAccessible(true);
                    return checkAndSelfOps(method.invoke(null, args.toArray()));
                } catch (Exception e) {
                    throw new EvaluateException("invoke target method failed", e).errStr(methodName);
                }
            } else {
                throw new EvaluateException("can not calculate,methodName and propertyName is null!");
            }
        } catch (BeanException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("calculate result failed,because ?", e);
        }
    }

    @Override
    public void assignFrom(Token assignment) {
        if (StringUtils.isEmpty(propertyName)) {
            String methodStr = getMethodStr();
            throw new EvaluateException("can not assign,because there is no reference or property").errStr(methodStr == null ? "=" : methodStr + "=");
        }
        try {
            Class<?> staticClass = Class.forName(opsClass);
            Field field = ReflectionUtils.getFieldRecursion(staticClass, propertyName);
            field.setAccessible(true);
            field.set(null, assignment.calculateResult(field.getType()));
        } catch (Exception e) {
            throw new EvaluateException("calculate result failed", e);
        }
    }
}
