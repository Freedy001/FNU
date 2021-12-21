package com.freedy.tinyFramework.Expression.token;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Freedy
 * @date 2021/12/20 21:38
 */
@AllArgsConstructor
@Getter
public final class WrapperToken extends Token{
    Token token;

}
