package com.freedy.tinyFramework.beanDefinition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author Freedy
 * @date 2021/12/2 15:46
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NormalBeanDefinition extends BeanDefinition {
    Boolean isRest=false;
    Boolean isConfigure=false;
    Boolean isProperties=false;
    public NormalBeanDefinition(String beanName, Class<?> beanClass) {
        super(beanName, beanClass);
    }
}
