package com.freedy;

/**
 * @author Freedy
 * @date 2021/11/20 10:15
 */

public record CMDParamParser(String[] mainArgs) {

    public Configuration parse() {
        Configuration configuration = new Configuration();


        return configuration;
    }
}
