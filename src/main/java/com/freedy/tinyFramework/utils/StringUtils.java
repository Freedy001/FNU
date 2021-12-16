package com.freedy.tinyFramework.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Freedy
 * @date 2021/12/2 16:27
 */
public class StringUtils {


    public static boolean hasText(String s) {
        return s != null && !s.equals("");
    }

    public static boolean isEmpty(String s) {
        return s == null || s.equals("");
    }

    public static boolean hasAnyText(String... s) {
        for (String s1 : s) {
            if (hasText(s1)) return true;
        }
        return false;
    }

    public static boolean isAnyEmpty(String... s) {
        for (String s1 : s) {
            if (isEmpty(s1)) return true;
        }
        return false;
    }

    private static String getUrl(String rawUrl) {
        rawUrl = rawUrl.trim();
        if (!rawUrl.startsWith("/")) {
            rawUrl = "/" + rawUrl;
        }
        if (rawUrl.endsWith("/")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
        }
        return URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);
    }


    /**
     * 字段转换 <br/>
     * 转化规则如下： <br/>
     * LRUCache ===> LRU_CACHE <br/>
     * phoneNum ===> PHONE_NUM <br/>
     * connectJDBCByType ===> CONNECT_JDBC_BY_TYPE <br/>
     *
     * @param fieldName 字段
     * @return 转化后的值
     */
    public static String convertEntityFieldToConstantField(String fieldName) {
        List<String> splitName = new ArrayList<>();
        char[] chars = fieldName.toCharArray();
        int length = chars.length;
        int lastIndex = 0;
        for (int i = 0; i < length; i++) {
            if (isUppercase(chars[i])) {
                //大写
                splitName.add(fieldName.substring(lastIndex, i));
                lastIndex = i;
                if (isUppercase(chars[i + 1])) {
                    for (; i < length; i++) {
                        if (isLowercase(chars[i])) {
                            splitName.add(fieldName.substring(lastIndex, i - 1));
                            lastIndex = i - 1;
                            break;
                        }
                    }
                }
            }
        }
        splitName.add(fieldName.substring(lastIndex, length));
        StringBuilder joiner = new StringBuilder();
        for (String s : splitName) {
            if (s != null && !s.equals("")) {
                joiner.append(s.toUpperCase(Locale.ROOT)).append("_");
            }
        }
        joiner.deleteCharAt(joiner.length() - 1);
        return joiner.toString();
    }

    /**
     * 上面转换的逆过程
     */
    public static String convertConstantFieldToEntityField(String fieldName) {
        if (!fieldName.contains("_")) return fieldName;
        String s = fieldName.toLowerCase(Locale.ROOT);
        int lastIndex = 0;
        while (true) {
            int index = s.indexOf("_");
            if (index == -1) break;
            s = s.substring(lastIndex, index) + s.substring(index + 1, index + 2).toUpperCase(Locale.ROOT) + s.substring(index + 2);
        }
        return s;
    }


    /**
     * 是否是大写字符
     */
    public static boolean isUppercase(char c) {
        return c >= 65 && c <= 90;
    }

    /**
     * 是否是小写字符
     */
    public static boolean isLowercase(char c) {
        return c >= 97 && c <= 122;
    }

}
