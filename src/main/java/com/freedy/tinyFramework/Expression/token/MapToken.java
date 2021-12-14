package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Freedy
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MapToken extends Token{
    private String mapStr;
    private String relevantOpsName;
}
