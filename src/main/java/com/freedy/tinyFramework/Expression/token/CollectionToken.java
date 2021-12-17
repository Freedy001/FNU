package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JSONType(includes = {"type","value"})
public class CollectionToken extends Token {
    private String[] elements;
    private String relevantOpsName;
    private static final Pattern numeric = Pattern.compile("\\d+");


    public CollectionToken(String value) {
        super("collection", value);
    }

    @Override
    public Object doCalculate(Class<?> desiredType) {
        if (elements == null) {
            throw new EvaluateException("null elements").errStr(value);
        }
        if (ReflectionUtils.isSonInterface(desiredType, "java.util.Collection")) {
            if (StringUtils.hasText(relevantOpsName)) {
                throw new EvaluateException("relevant ops [?] are not allow", relevantOpsName).errStr(relevantOpsName);
            }
            //集合没指定泛型
            Collection<Object> result = ReflectionUtils.buildCollectionByType(desiredType);
            result.addAll(Arrays.asList(elements));
            return checkAndSelfOps(result);
        } else {
            //非集合
            if (StringUtils.hasText(relevantOpsName)) {
                if (!numeric.matcher(relevantOpsName).matches()) {
                    throw new EvaluateException("illegal ops [?], ops must be number", relevantOpsName).errStr(relevantOpsName);
                }
                return checkAndSelfOps(ReflectionUtils.convertType(elements[Integer.parseInt(relevantOpsName)], desiredType));
            } else {
                //无relevantOpsName todo
                throw new EvaluateException("need relevant ops ?[number]", value).errStr(value);
            }
        }
    }

    @Override
    protected Object doGenericCalculate(ParameterizedType desiredType) {
        if (StringUtils.hasText(relevantOpsName)) {
            throw new EvaluateException("relevant ops ? are not allow", relevantOpsName).errStr(relevantOpsName);
        }
        return checkAndSelfOps(ReflectionUtils.buildCollectionByTypeAndValue(desiredType, elements));
    }

}
