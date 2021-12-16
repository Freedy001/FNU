package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.Tokenizer;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JSONType(ignores = {"numeric","strPattern","context","desiredType","notFlag","preSelfAddFlag","preSelfSubFlag","postSelfAddFlag","postSelfSubFlag"})
public class MapToken extends Token {
    private String mapStr;
    private String relevantOpsName;
    private Pattern strPattern = Pattern.compile("^'(.*?)'$");

    public MapToken(String value) {
        super("map", value);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        if (ReflectionUtils.isSonInterface(desiredType, "java.util.Map")) {
            if (StringUtils.hasText(relevantOpsName)) {
                throw new EvaluateException("relevant ops [?] are not allow", relevantOpsName).errStr(relevantOpsName);
            }
            //没指定泛型的map
            JSONObject jsonObject = JSON.parseObject(mapStr);
            HashMap<String, String> map = new HashMap<>();
            for (String s : jsonObject.keySet()) {
                map.put(s, jsonObject.getString(s));
            }
            return checkAndSelfOps(map);
        } else {
            //具体值
            if (StringUtils.isEmpty(relevantOpsName)) {
                throw new EvaluateException("need relevant ops ?[ops]", value).errStr(value);
            }
            JSONObject jsonObject = JSON.parseObject(mapStr);
            Matcher matcher = strPattern.matcher(relevantOpsName);
            if (matcher.find()) {
                return checkAndSelfOps(ReflectionUtils.convertType(jsonObject.getString(matcher.group(1)), desiredType));
            }
            checkContext();
            //解析字串
            TokenStream tokenStream = new Tokenizer().getTokenStream(relevantOpsName);
            Expression expression = new Expression(tokenStream);
            String key = expression.getValue(context, String.class);
            return checkAndSelfOps(ReflectionUtils.convertType(jsonObject.getString(key), desiredType));
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object doGenericCalculate(ParameterizedType desiredType) {
        try {
            Type[] genericType = desiredType.getActualTypeArguments();
            Class<?> _1stType = (Class<?>) genericType[0];
            Class<?> _2ndType = (Class<?>) genericType[1];
            Class<?> rawType = (Class<?>) desiredType.getRawType();
            Map<Object, Object> map;
            if (!rawType.isInterface()) {
                //非interface
                map = (Map<Object, Object>) rawType.getConstructor().newInstance();
            } else {
                map = new HashMap<>();
            }
            JSONObject jsonObject = JSON.parseObject(mapStr);
            for (String s : jsonObject.keySet()) {
                map.put(ReflectionUtils.convertType(s,_1stType), ReflectionUtils.convertType(jsonObject.getString(s),_2ndType));
            }
            return checkAndSelfOps(map);
        } catch (Exception e) {
           throw new IllegalArgumentException("create generic failed,because ?",e);
        }
    }

}
