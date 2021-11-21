package com.freedy;

/**
 * @author Freedy
 * @date 2021/11/20 10:15
 */

public record CMDParamParser(String[] mainArgs) {

    public Configuration parse() {
        Configuration configuration = new Configuration();
        if (mainArgs.length>0&&mainArgs[0] != null) {
            configuration.setPropertiesPath(mainArgs[0]);
            System.out.println("配置文件路径" + configuration.getPropertiesPath());
        }
        return configuration;
    }
}
