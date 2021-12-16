package com.freedy.tinyFramework.Expression.token;

/**
 * @author Freedy
 * @date 2021/12/16 9:11
 */
public interface Assignable {

    /**
     * 实现此接口的token表示能够被分配变量
     */
    void assignFrom(Token assignment);

}
