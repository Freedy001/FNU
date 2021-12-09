package com.freedy.tinyFramework.beanDefinition;

import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Method;

/**
 * @author Freedy
 * @date 2021/12/4 14:37
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ConfigBeanDefinition extends BeanDefinition {

    private Method beanFactoryMethod;
    private Bean beanInfo;

    public ConfigBeanDefinition(Method beanFactoryMethod,Bean beanInfo) {
        super(StringUtils.hasText(beanInfo.value())?beanInfo.value():beanFactoryMethod.getName(), beanFactoryMethod.getReturnType());
        this.beanFactoryMethod = beanFactoryMethod;
        this.beanInfo = beanInfo;
    }

}
