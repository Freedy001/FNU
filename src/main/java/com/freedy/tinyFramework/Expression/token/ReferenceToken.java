package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.Tokenizer;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JSONType(ignores = {"numeric","strPattern","context","desiredType","notFlag","preSelfAddFlag","preSelfSubFlag","postSelfAddFlag","postSelfSubFlag"})
public class ReferenceToken extends ClassToken {
    private String referenceName;
    private Pattern strPattern = Pattern.compile("^'(.*?)'$");
    private Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");


    public ReferenceToken(String token) {
        super("reference", token);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        if (StringUtils.isEmpty(referenceName)) {
            throw new EvaluateException("reference is null");
        }
        checkContext();
        if (StringUtils.hasText(propertyName)) {
            Object variable = context.getVariable(referenceName);
            if (variable == null) {
                throw new EvaluateException("can not calculate,because reference can not find a variable in context").errStr(referenceName);
            }
            return checkAndSelfOps(ReflectionUtils.getter(variable, propertyName));
        }
        try {
            if (StringUtils.hasText(methodName)) {
                Object variable = context.getVariable(referenceName);
                if (variable == null) {
                    throw new EvaluateException("can not calculate,because reference can not find a variable in context").errStr(referenceName);
                }
                List<Object> args = new ArrayList<>();
                for (String methodArg : methodArgs) {
                    Matcher matcher = strPattern.matcher(methodArg);
                    if (matcher.find()){
                        args.add(matcher.group(1));
                        continue;
                    }
                    matcher=numeric.matcher(methodArg);
                    if (matcher.matches()){
                        args.add(methodArg.matches(".*?[lL]$")?Long.parseLong(methodArg):Integer.parseInt(methodArg));
                        continue;
                    }
                    Tokenizer tokenizer = new Tokenizer();
                    TokenStream stream = tokenizer.getTokenStream(methodArg);
                    Expression expression = new Expression(stream);
                    args.add(expression.getValue(context));
                }
                Method method = variable.getClass().getMethod(methodName, args.stream().map(Object::getClass).toArray(Class[]::new));
                method.setAccessible(true);
                return method.invoke(variable,args.toArray());
            }else {
                //todo
                throw new EvaluateException("can not calculate,methodName and propertyName is null!").errStr(value);
            }
        } catch (Exception e) {
            throw new EvaluateException("invoke target method failed,because ?",e);
        }
    }

    @Override
    public void assignFrom(Token assignment) {
        checkContext();
        if (StringUtils.isAnyEmpty(referenceName, propertyName)) {
            throw new EvaluateException("can not assign,because reference or property is null");
        }
        Object variable = context.getVariable(referenceName);
        if (variable == null) {
            throw new EvaluateException("can not assign,because reference can not find a variable in context");
        }
        Type desiredType = ReflectionUtils.getFieldRecursion(variable.getClass(), propertyName).getGenericType();
        ReflectionUtils.setter(variable, propertyName, assignment.calculateResult(desiredType));
    }
}
