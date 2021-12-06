package com.freedy.tinyFramework.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/11/30 10:09
 */
public class DateUtils {

    private final static List<Pattern> REG_PATTERN_LIST = Arrays.asList(
            //yyyy-MM-dd hh:mm:ss 中间的'-'为任意非数字字符
            Pattern.compile("^([\\d\\w][\\d\\w][\\d\\w][\\d\\w])\\D?([\\d\\w]{1,2})\\D?([\\d\\w]{1,2}) +([\\d\\w]{1,2}):([\\d\\w]{1,2}):([\\d\\w]{1,2})$"),
            //yyyy-MM-dd 中间的'-'为任意非数字字符
            Pattern.compile("^([\\d\\w][\\d\\w][\\d\\w][\\d\\w])\\D?([\\d\\w]{1,2})\\D?([\\d\\w]{1,2})$"),
            //MM-dd-yyyy 中间的'-'为任意非数字字符
            Pattern.compile("^([\\d\\w]{1,2})\\D?([\\d\\w]{1,2})\\D?([\\d\\w][\\d\\w][\\d\\w][\\d\\w])$"),
            //hh:mm:ss MM-dd-yyyy 中间的'-'为任意非数字字符
            Pattern.compile("^([\\d\\w]{1,2}):([\\d\\w]{1,2}):([\\d\\w]{1,2}) +([\\d\\w]{1,2})\\D?([\\d\\w]{1,2})\\D?([\\d\\w][\\d\\w][\\d\\w][\\d\\w])$")
    );

    private final static String[][] SORTED_2CHARS_ARR = {
            //regPatternList index为0时的Pattern顺序
            {"MM", "dd", "hh", "mm", "ss"},
            //regPatternList index为1时的Pattern顺序
            {"MM", "dd"},
            //regPatternList index为2时的Pattern顺序
            {"MM", "dd"},
            //regPatternList index为3时的Pattern顺序
            {"hh", "mm", "ss", "MM", "dd"}
    };

    /**
     * 查找时间字符串正则表达式对象
     */
    private static Pattern findStrRegPattern(String str) {
        for (Pattern pattern : REG_PATTERN_LIST) {
            if (pattern.matcher(str).matches())
                return pattern;
        }
        throw new UnsupportedOperationException("请在patternList加入与你输入匹配的pattern");
    }

    /**
     * 获取时间字符串pattern
     */
    public static String getDateStrPattern(String str) {
        Pattern pattern = findStrRegPattern(str);
        Matcher matcher = pattern.matcher(str);
        int patternIndex = REG_PATTERN_LIST.indexOf(pattern);
        if (matcher.find()) {
            int count = matcher.groupCount();
            for (int i = 1, _2charIndex = 0; i <= count; i++) {
                String gStr = matcher.group(i);
                if (gStr.length() == 4)
                    str = str.replaceFirst(gStr, "yyyy");
                else
                    str = str.replaceFirst(gStr, SORTED_2CHARS_ARR[patternIndex][_2charIndex++]);
            }
        }
        return str;
    }

    /**
     * 重复指定的字符串指定的次数
     */
    private static String repeat(String str, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) builder.append(str);
        return builder.toString();
    }

    /**
     * 转换时间字符串为你所指定的pattern
     */
    public static String convertDateStrToYourPattern(String dateStr, String yourPattern) {
        String finalStr = dateStr.toLowerCase(Locale.ROOT).trim();
        Matcher dateMatcher = findStrRegPattern(finalStr).matcher(finalStr);
        Matcher convertMatcher = findStrRegPattern(yourPattern).matcher(yourPattern);
        if (dateMatcher.find() && convertMatcher.find()) {
            int dCount = dateMatcher.groupCount();
            int cCount = convertMatcher.groupCount();
            if (dCount > cCount) {
                for (int i = 1; i <= cCount; i++) {
                    yourPattern = yourPattern.replaceFirst(convertMatcher.group(i), dateMatcher.group(i));
                }
            } else {
                for (int i = 1; i <= dCount; i++) {
                    yourPattern = yourPattern.replaceFirst(convertMatcher.group(i), dateMatcher.group(i));
                }
                for (int i = dCount + 1; i <= cCount; i++) {
                    String group = convertMatcher.group(i);
                    yourPattern = yourPattern.replaceFirst(group, repeat("0", group.length()));
                }
            }
        } else {
            throw new IllegalArgumentException("请检查patternList中的pattern是否符合要求");
        }

        return yourPattern;
    }

    /**
     * 常规自动日期格式识别
     */
    public static Date getDate(String str) {
        SimpleDateFormat format = new SimpleDateFormat(getDateStrPattern(str));
        try {
            return format.parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

}
