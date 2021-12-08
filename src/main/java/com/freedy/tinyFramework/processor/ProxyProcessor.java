package com.freedy.tinyFramework.processor;

import com.freedy.tinyFramework.beanDefinition.ProxyBeanDefinition;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import com.freedy.tinyFramework.exception.ProxyExecuteException;
import lombok.Data;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2021/12/7 14:42
 */
public class ProxyProcessor {

    private final Object bean;
    private final ClassLoader beanClassLoader;
    private final BeanFactory factory;
    private final List<ProxyBeanDefinition.MataInterceptor> interceptorList;


    public ProxyProcessor(Object bean, BeanFactory factory, List<ProxyBeanDefinition.MataInterceptor> interceptorList) {
        this.beanClassLoader = bean.getClass().getClassLoader();
        this.bean = bean;
        this.factory = factory;
        this.interceptorList = interceptorList;
    }

    public Object getProxy() {
        Class<?> beanClass = bean.getClass();
        Class<?>[] beanClassInterfaces = beanClass.getInterfaces();
        if (beanClassInterfaces.length > 0) {
            return new ProxyMataInfo(jdkDynamicProxy(beanClassInterfaces), ProxyType.JDK_TYPE);
        }
//        if (beanClass.getSuperclass() == Object.class) {
            //可能因为jdk17的原因 cglib不支持对有父类的类进行动态代理。
            return new ProxyMataInfo(cglibDynamicProxy(beanClass), ProxyType.CGLIB_TYPE);
//        }
//        throw new UnsupportedOperationException(new PlaceholderParser("detect target class[name:?] are aimed by interception expression[val:?],but target class are not support to be proxied,please add a interface to target class", beanClass.getName(), interceptorList.get(0).getRowClassEl()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN).toString());
    }


    private Object jdkDynamicProxy(Class<?>[] beanClassInterfaces) {
        return Proxy.newProxyInstance(beanClassLoader, beanClassInterfaces, (proxy, method, args) -> executeTargetAndInterceptor(method, args));
    }

    private Object cglibDynamicProxy(Class<?> beanClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(beanClass);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> executeTargetAndInterceptor(method, args));
        return enhancer.create();
    }


    public Object executeTargetAndInterceptor(Method method, Object[] args) throws Exception {
        Map<String, List<ProxyBeanDefinition.MataInterceptor>> typeMataMapping = interceptorList.stream()
                .filter(mata -> evaluateExpressionLanguage(mata, method))
                .collect(Collectors.groupingBy(ProxyBeanDefinition.MataInterceptor::getType));

        InterceptorOperation operation = new InterceptorOperation(method, args, bean);
        //pre
        List<ProxyBeanDefinition.MataInterceptor> preList = typeMataMapping.get("com.freedy.tinyFramework.annotation.interceptor.Pre");
        if (preList != null) {
            operation.setType("com.freedy.tinyFramework.annotation.interceptor.Pre");
            for (ProxyBeanDefinition.MataInterceptor item : preList) {
                Method m = item.getInterceptMethod();
                Object interceptorBean = factory.getBean(m.getDeclaringClass());
                Object[] arguments = getArguments(m, operation);
                operation.setLastReturnVal(m.invoke(interceptorBean, arguments));
            }

        }
        //around
        List<ProxyBeanDefinition.MataInterceptor> aroundList = typeMataMapping.get("com.freedy.tinyFramework.annotation.interceptor.Around");
        operation.setType("com.freedy.tinyFramework.annotation.interceptor.Around");
        if (aroundList != null) {
            for (ProxyBeanDefinition.MataInterceptor item : aroundList) {
                operation.registerExecutorChain(() -> {
                    Method m = item.getInterceptMethod();
                    Object interceptorBean = factory.getBean(m.getDeclaringClass());
                    Object[] arguments = getArguments(m, operation);
                    try {
                        return m.invoke(interceptorBean, arguments);
                    } catch (Exception e) {
                        throw new ProxyExecuteException("invoke interceptor method[name:?] failed!because ?", m, e);
                    }
                });
            }
        }
        //执行方法
        Object returnVal = operation.invokeTarget();
        operation.setLastReturnVal(returnVal);
        //after
        List<ProxyBeanDefinition.MataInterceptor> afterList = typeMataMapping.get("com.freedy.tinyFramework.annotation.interceptor.After");
        if (afterList != null) {
            operation.setType("com.freedy.tinyFramework.annotation.interceptor.After");
            for (ProxyBeanDefinition.MataInterceptor item : afterList) {
                Method m = item.getInterceptMethod();
                Object interceptorBean = factory.getBean(m.getDeclaringClass());
                Object[] arguments = getArguments(m, operation);
                operation.setLastReturnVal(m.invoke(interceptorBean, arguments));
            }
        }

        return returnVal;
    }


    private Object[] getArguments(Method m, InterceptorOperation operation) {
        int count = m.getParameterCount();
        Object[] args = new Object[count];
        Class<?>[] parameterTypes = m.getParameterTypes();
        for (int i = 0; i < count; i++) {
            if (parameterTypes[i] == operation.getClass()) {
                args[i] = operation;
            } else {
                args[i] = factory.getBean(parameterTypes[i]);
            }
        }
        return args;
    }

    //com.freedy.tinyFramework.processor.ProxyProcessor.get*(int,Method,*)
    private boolean evaluateExpressionLanguage(ProxyBeanDefinition.MataInterceptor el, Method method) {
        return Pattern.compile(el.getMethodNameEl()).matcher(method.getName()).matches() &&
                Pattern.compile(el.getMethodArgEl()).matcher(
                        Arrays.stream(method.getParameterTypes())
                                .map(Class::getSimpleName).collect(Collectors.joining(","))
                ).matches();
    }


    @Data
    public static class ProxyMataInfo {
        private Object proxyObj;
        private ProxyType proxyType;
        private Object originObj;

        public ProxyMataInfo(Object proxyObj, ProxyType proxyType) {
            this.proxyObj = proxyObj;
            this.proxyType = proxyType;
        }
    }

    public enum ProxyType {
        JDK_TYPE,
        CGLIB_TYPE
    }

}
