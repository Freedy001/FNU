package com.freedy.tinyFramework.beanFactory;

import com.freedy.tinyFramework.exception.NoUniqueBeanException;
import com.freedy.tinyFramework.processor.ProxyProcessor;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 默认bean工厂 持有bean的容器
 *
 * @author Freedy
 * @date 2021/12/4 10:04
 */
@Slf4j
public abstract class DefaultBeanFactory implements BeanFactory {

    private final Map<String, Object> singletonObject = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> typeSingletonObject = new ConcurrentHashMap<>();
    private final List<Consumer<BeanFactory>> notifyList=new ArrayList<>();

    protected void registerBean(String beanName, Object bean) {
        if (bean instanceof ProxyProcessor.ProxyMataInfo mataInfo) {
            if (mataInfo.getProxyType() == ProxyProcessor.ProxyType.CGLIB_TYPE) {
                //cglib直接注入代理对象即可
                bean = mataInfo.getProxyObj();
            } else if (mataInfo.getProxyType() == ProxyProcessor.ProxyType.JDK_TYPE) {
                Object originObj = mataInfo.getOriginObj();
                putInterfaceAndSuperclass(beanName, originObj);
            }
        }
        Object containerBean = singletonObject.put(beanName, bean);
        log.debug("register bean name:{},type:{}", beanName, bean.getClass().getName());
        if (containerBean != null)
            throw new NoUniqueBeanException("same bean name!your bean " + bean + " container bean " + containerBean);
        putInterfaceAndSuperclass(beanName, bean);
        notifyList.forEach(item->item.accept(this));
    }

    public void registerBeanAddNotifier(Consumer<BeanFactory> notifier){
        notifyList.add(notifier);
    }


    private void putInterfaceAndSuperclass(String beanName, Object originObj) {
        for (Class<?> sClass : ReflectionUtils.getClassRecursion(originObj)) {
            if (!sClass.getName().equals("java.lang.Object")) {
                //添加所有父类到容器中
                typeSingletonObject.merge(sClass, beanName, (o, n) -> o + "," + n);
            }
        }
        for (Class<?> aClass : ReflectionUtils.getInterfaceRecursion(originObj)) {
            //添加所有接口到容器中
            typeSingletonObject.merge(aClass, beanName, (o, n) -> o + "," + n);
        }
    }

    @Override
    public Object getBean(String beanName) {
        Object bean = singletonObject.get(beanName);
        if (bean instanceof ProxyProcessor.ProxyMataInfo proxyMataInfo) {
            return proxyMataInfo.getProxyObj();
        }
        return bean;
    }

    @Override
    public <T> T getBean(String beanName, Class<T> beanType) {
        Object bean = singletonObject.get(beanName);
        if (bean instanceof ProxyProcessor.ProxyMataInfo proxyMataInfo &&
                proxyMataInfo.getProxyType() == ProxyProcessor.ProxyType.JDK_TYPE) {
            bean = proxyMataInfo.getProxyObj();
            if (!beanType.isInstance(bean)) {
                bean = proxyMataInfo.getOriginObj();
                if (beanType.isInstance(bean)) {
                    log.warn("""
                            It is detected that the bean[name:{},type{}] you want to get is a bean that needs to be JDK-dynamic proxied,
                            but the type you use to receive the bean is a non interface type. So this bean will not be enhanced!
                            """, beanName, beanType.getName());
                }
            }
        }
        return beanType.cast(bean);
    }


    @Override
    public <T> T getBean(Class<T> beanType) {
        String[] beanNames = Optional.ofNullable(typeSingletonObject.get(beanType)).orElse("").split(",");
        int length = beanNames.length;
        if (length == 0) return null;
        if (length > 1) {
            throw new NoUniqueBeanException("find ? bean definition[type:?] in the bean definition container!", length, beanType.getName());
        }
        return beanType.cast(getBean(beanNames[0], beanType));
    }

    @Override
    public <T> List<T> getBeansForType(Class<T> beanType) {
        String[] beanNames = Optional.ofNullable(typeSingletonObject.get(beanType)).orElse("").split(",");
        ArrayList<T> ts = new ArrayList<>();
        for (String beanName : beanNames) {
            if (StringUtils.hasText(beanName)) {
                ts.add(beanType.cast(getBean(beanName)));
            }
        }
        return ts;
    }


    @Override
    public Set<String> getAllBeanNames() {
        return singletonObject.keySet();
    }

    @Override
    public boolean containsBean(String beanName) {
        return singletonObject.containsKey(beanName);
    }

    @Override
    public boolean containsBean(Class<?> beanType) {
        return typeSingletonObject.containsKey(beanType);
    }
}
