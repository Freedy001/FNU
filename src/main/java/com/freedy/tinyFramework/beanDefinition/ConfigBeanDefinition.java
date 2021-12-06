package com.freedy.tinyFramework.beanDefinition;

import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;

/**
 * @author Freedy
 * @date 2021/12/4 14:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigBeanDefinition extends BeanDefinition {

    private Method beanFactoryMethod;
    private Bean beanInfo;

    public ConfigBeanDefinition(Method beanFactoryMethod,Bean beanInfo) {
        super(beanFactoryMethod.getName(), beanFactoryMethod.getReturnType());
        this.beanFactoryMethod = beanFactoryMethod;
        this.beanInfo = beanInfo;
    }

}
