package com.freedy.tinyFramework;

import com.freedy.tinyFramework.beanFactory.AbstractApplication;

/**
 * @author Freedy
 * @date 2021/12/4 22:41
 */
public interface Scanner {
    void scan(String ...packagesName);

    void scan(String[] packagesName,String[] exclude);

    AbstractApplication getApplication();

    void setApplication(AbstractApplication application);
}
