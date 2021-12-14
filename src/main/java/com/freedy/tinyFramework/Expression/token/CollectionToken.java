package com.freedy.tinyFramework.Expression.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionToken extends Token {
    private String[] elements;
    private String relevantOpsName;
}