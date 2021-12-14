package com.freedy.tinyFramework.beanDefinition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Freedy
 * @date 2021/12/5 15:59
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PropertiesBeanDefinition extends BeanDefinition{
    private String prefix;
    private boolean nonePutIfEmpty;
    private String[] exclude;

    public PropertiesBeanDefinition(String beanName, Class<?> beanClass, String prefix,boolean nonePutIfEmpty,String[] exclude) {
        super(beanName, beanClass);
        this.prefix = prefix;
        this.nonePutIfEmpty=nonePutIfEmpty;
        this.exclude=exclude;
    }
}
