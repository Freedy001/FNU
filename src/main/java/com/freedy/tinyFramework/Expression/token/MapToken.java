package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MapToken extends Token {
    private String mapStr;
    private String relevantOpsName;
    private Pattern strPattern=Pattern.compile("^'(.*?)'$");
    public MapToken( String value) {
        super("map", value);
    }


}
