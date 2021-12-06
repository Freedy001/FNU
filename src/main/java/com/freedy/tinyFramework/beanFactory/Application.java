package com.freedy.tinyFramework.beanFactory;

import com.freedy.tinyFramework.BeanDefinitionScanner;
import com.freedy.tinyFramework.beanDefinition.BeanDefinition;
import com.freedy.tinyFramework.beanDefinition.NormalBeanDefinition;
import com.freedy.tinyFramework.processor.RestProcessor;

/**
 * @author Freedy
 * @date 2021/12/2 15:39
 */
public class Application extends AbstractApplication {

    private final BeanDefinitionScanner scanner;
    private RestProcessor restProcessor;


    public Application(Class<?> baseClass) {
        scanner = new BeanDefinitionScanner(baseClass, this);
        setBean(this);
    }


    public Application run() {
        productBean();
        return this;
    }

    public Application startRest() {
        restProcessor=new RestProcessor();
        for (BeanDefinition definition : beanDefinition.values()) {
            if (definition instanceof NormalBeanDefinition nbd && nbd.getIsRest()) {
                restProcessor.registerController(getBean(definition.getBeanName()));
            }
        }
        return this;
    }


    public void registerRestInner(Object obj){
        if (restProcessor==null) throw new IllegalArgumentException("please start rest server first!");
        restProcessor.registerInnerObj(obj);
    }


    public void scan(String... packageNames) {
        scanner.scan(packageNames);
    }


    @Override
    protected void beanDefinitionPostProcess(BeanDefinition beanTypeDefinition) {

    }

    @Override
    protected Object beanBeforeCreatedPostProcess(BeanDefinition definition) {
        return null;
    }
}
