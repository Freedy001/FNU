package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReferenceToken extends ClassToken {
    private String referenceName;
    private Pattern strPattern = Pattern.compile("^'(.*?)'$");
    private Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");

    public ReferenceToken(String token) {
        super("reference", token);
    }


}
