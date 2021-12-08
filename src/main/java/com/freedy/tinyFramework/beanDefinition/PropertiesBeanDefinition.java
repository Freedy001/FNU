package com.freedy.tinyFramework.beanDefinition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Freedy
 * @date 2021/12/5 15:59
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PropertiesBeanDefinition extends BeanDefinition{
    String prefix;


    public PropertiesBeanDefinition(String beanName, Class<?> beanClass, String prefix) {
        super(beanName, beanClass);
        this.prefix = prefix;
    }
}
