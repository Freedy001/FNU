package com.freedy.tinyFramework.beanFactory;

import com.freedy.tinyFramework.annotation.beanContainer.Bean;
import com.freedy.tinyFramework.annotation.beanContainer.BeanType;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.beanDefinition.BeanDefinition;
import com.freedy.tinyFramework.beanDefinition.ConfigBeanDefinition;
import com.freedy.tinyFramework.beanDefinition.PropertiesBeanDefinition;
import com.freedy.tinyFramework.beanDefinition.ProxyBeanDefinition;
import com.freedy.tinyFramework.exception.*;
import com.freedy.tinyFramework.processor.PropertiesExtractor;
import com.freedy.tinyFramework.processor.ProxyProcessor;
import com.freedy.tinyFramework.utils.LockProvider;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Freedy
 * @date 2021/12/2 15:53
 */
@Slf4j
public abstract class AbstractApplication extends DefaultBeanFactory {


    private final Map<String, Object> earlySingletonObject = new ConcurrentHashMap<>();

    protected final Map<String, BeanDefinition> beanDefinition = new ConcurrentHashMap<>();
    protected final Map<Class<?>, List<BeanDefinition>> beanTypeDefinition = new ConcurrentHashMap<>();
    protected final PropertiesExtractor propertiesExtractor = new PropertiesExtractor();

    public void registerBeanDefinition(BeanDefinition b) {
        BeanDefinition containerBeanDef = beanDefinition.put(b.getBeanName(), b);
        if (containerBeanDef != null) {
            throw new NoUniqueBeanException("same beanDefinition name[?]! your beanDefinition[type:?,beanType:?] container beanDefinition:[type:?,beanType:?] ",b.getBeanName(), b.getClass().getSimpleName(), b.getBeanClass().getName(), containerBeanDef.getClass().getSimpleName(), containerBeanDef.getBeanClass().getName());
        }
        Set<Class<?>> set = new HashSet<>();
        for (Class<?> superClass : ReflectionUtils.getClassRecursion(b.getBeanClass())) {
            if (superClass.getName().equals("java.lang.Object")) continue;
            beanTypeDefinition.computeIfAbsent(superClass, k -> new ArrayList<>()).add(b);
            set.addAll(ReflectionUtils.getInterfaceRecursion(superClass));
        }
        for (Class<?> interfaceClass : set) {
            beanTypeDefinition.computeIfAbsent(interfaceClass, k -> new ArrayList<>()).add(b);
        }
    }


    protected void productBean() {
        LockProvider.initContainer.enterCritical();

        //??????BeanDefinition?????????????????????
        beanDefinition.forEach((name, definition) -> {
            beanDefinitionPostProcess(definition);
            //????????????
            if (definition.getType() == BeanType.SINGLETON && !containsBean(name)) {
                //??????????????????
                getBean(definition.getBeanName());
            }
        });

        LockProvider.initContainer.exitCritical();
    }

    @SneakyThrows
    public void injectBean(@NonNull Object bean) {
        for (Field field : ReflectionUtils.getFieldsRecursion(bean.getClass())) {
            Inject inject = field.getAnnotation(Inject.class);
            if (inject == null) continue;
            Class<?> type = field.getType();

            //by name
            String byName = inject.value();
            if (StringUtils.hasText(byName)) {
                field.setAccessible(true);
                Object dependency = getBean(byName);
                if (dependency == null) {
                    log.error("can not find bean[name:{},type:{}] in the container.", byName, type.getName());
                }
                field.set(bean, dependency);
                continue;
            }

            //by type
            Object dependency = getBean(type, field.getName());
            if (dependency == null) {
                log.error("can not find bean[type:{}] in the container.", type.getName());
            }
            field.set(bean, dependency);
        }
    }

    private List<Object> getArgumentsFromBeanContainer(Executable method) {
        method.setAccessible(true);
        List<Object> arguments = new ArrayList<>(method.getParameterCount());
        for (Parameter parameter : method.getParameters()) {
            Class<?> type = parameter.getType();
            Inject inject = parameter.getAnnotation(Inject.class);
            String paramName = parameter.getName();
            arguments.add(getBean(type, inject == null ? paramName : StringUtils.hasText(inject.value()) ? inject.value() : paramName));
        }
        return arguments;
    }


    public <T> T getBean(Class<T> beanType) {
        return beanType.cast(getBean(beanType, null));
    }

    /**
     * ??????????????????
     */
    private Object getBean(Class<?> beanType, String beanName) {
        try {
            //????????? bean???????????????
            Object bean = super.getBean(beanType);
            if (bean != null) return bean;
        } catch (Exception ignored) {
            //do nothing
        }

        List<?> list = getBeansForType(beanType);
        if (list == null || list.size() == 0) {
            //???????????????
            List<BeanDefinition> definitions = beanTypeDefinition.get(beanType);
            if (definitions == null || definitions.size() == 0) {
                throw new NoSuchBeanException("can not find bean-definition[type:?] in the container", beanType.getName());
            }
            //??????/???????????? ????????????
            if (definitions.size() == 1) return getBean(definitions.get(0).getBeanName());
            if (beanName == null) {
                throw new NoUniqueBeanException("find ? bean definition[type:?] in the bean definition container! please specify one!", definitions.size(), beanType.getName());
            }
            BeanDefinition definition = this.beanDefinition.get(beanName);
            //???????????????????????????????????????bean definition
            if (definition != null && definition.getBeanClass() == beanType) return getBean(definition.getBeanName());
            throw new NoUniqueBeanException("find ? bean definition[type:?] in the bean definition container! please specify one!", definitions.size(), beanType.getName());
        }
        if (list.size() == 1) return list.get(0);
        if (beanName == null) {
            throw new NoUniqueBeanException("find ? bean[type:?] in the bean definition container! please specify one!", list.size(), beanType.getName());
        }
        Object bean = getBean(beanName);
        if (beanType.isInstance(bean)) return bean;
        throw new NoUniqueBeanException("find ? bean[type:?] in the bean definition container! please specify one!", list.size(), beanType.getName());
    }


    public Object getBean(String beanName) {
        Object bean = super.getBean(beanName);
        if (bean != null) return bean;

        synchronized (this) {
            //????????????
            if ((bean = super.getBean(beanName)) != null) return bean;

            //????????????????????????beanName??????????????????????????????
            if ((bean = earlySingletonObject.get(beanName)) != null) return bean;

            //??????bean
            BeanDefinition definition = this.beanDefinition.get(beanName);
            if (definition == null) {
                throw new NoSuchBeanException("no bean definition[name:?] in the container", beanName);
            }

            if ((bean = beanBeforeCreatedPostProcess(definition)) != null) return bean;

            try {
                //bean????????????-??????????????????
                bean = createBeanByConstructor(definition);
            } catch (Exception e) {
                throw new BeanInitException("create bean[name:?] failed,because ?", beanName, e);
            }

            //????????????????????????null
            if (bean == null) return null;

            //???????????????????????????
            Object proxyBean = checkProxy(bean, definition);

            if (proxyBean instanceof ProxyProcessor.ProxyMataInfo mataInfo) {
                //??????????????????
                mataInfo.setOriginObj(bean);
            }

            //????????????????????????????????????????????????
            earlySingletonObject.put(beanName, proxyBean);

            for (Field field : ReflectionUtils.getFieldsRecursion(definition.getBeanClass())) {
                Inject inject = field.getAnnotation(Inject.class);
                if (inject == null) continue;
                field.setAccessible(true);
                Class<?> type = field.getType();

                //by name inject
                String dependencyBeanName = inject.value();
                Object dependency;
                if (StringUtils.hasText(dependencyBeanName)) {
                    //??????bean
                    dependency = getBean(dependencyBeanName);
                    if (dependency == null) {
                        log.error("can not find bean[name:{},type:{}] in the container.", dependencyBeanName, type.getName());
                    }
                    try {
                        //????????????????????????bean????????????
                        dependency = checkAndConvertProxyType(type, dependency, dependencyBeanName);
                        //????????????
                        field.set(bean, dependency);
                    } catch (Exception e) {
                        throw new InjectException("inject field ? failed,because ?", field.getName(), e);
                    }
                    continue;
                }
                //by type inject
                dependency = getBean(type, field.getName());
                if (dependency == null) {
                    log.error("can not find bean[type:{}] in the container.", type.getName());
                }
                try {
                    //????????????????????????bean????????????
                    dependency = checkAndConvertProxyType(type, dependency, dependencyBeanName);
                    //????????????
                    field.set(bean, dependency);
                } catch (Exception e) {
                    throw new InjectException("inject field ? failed,because ?", field.getName(), e);
                }
            }
            try {
                //??????PostConstruct ??? inject??????
                invokePostConstruct(bean, definition);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            //????????????????????????????????????????????????
            earlySingletonObject.remove(beanName);

            //??????????????????
            if (definition.getType() == BeanType.SINGLETON) {
                //????????????
                registerBean(beanName, proxyBean);
            } else {
                log.debug("produce a PROTOTYPE bean({})", beanName);
            }

            if (proxyBean instanceof ProxyProcessor.ProxyMataInfo proxyMataInfo)
                return proxyMataInfo.getProxyObj();
            else
                return proxyBean;
        }
    }

    private Object checkAndConvertProxyType(Class<?> fieldDependencyType, Object dependency, String dependencyBeanName) {
        if (dependency == null) return null;
        if (!fieldDependencyType.isInstance(dependency)) {
            //?????????????????????jdk????????????????????????
            if (dependency instanceof ProxyProcessor.ProxyMataInfo mataInfo) {
                //???????????????????????????????????? ?????????????????????????????????????????????????????????????????????ProxyMataInfo??????
                dependency = mataInfo.getProxyObj();
                if (!fieldDependencyType.isInstance(dependency)) {
                    //??????????????????????????????
                    dependency = mataInfo.getOriginObj();
                    if (fieldDependencyType.isInstance(dependency)) {
                        //????????????
                        log.warn("""
                                It is detected that the bean[name:{},type{}] you want to get is a bean that needs to be JDK-dynamic proxied,
                                but the type you use to receive the bean is a non interface type. So this bean will not be enhanced!
                                """, dependencyBeanName, fieldDependencyType.getName());
                    }
                }
            } else {
                //????????????????????????????????????
                dependency = getBean(dependencyBeanName, fieldDependencyType);
            }
        }
        return dependency;
    }

    private Object createBeanByConstructor(BeanDefinition definition) {
        try {
            Object bean = null;
            boolean hasConstruct = false;
            //??????ConfigBeanDefinition
            if (definition instanceof ConfigBeanDefinition configDefinition) {
                Bean info = configDefinition.getBeanInfo();
                boolean missCondition = false;
                creteConfigBean:
                {
                    String beanByName = info.conditionalOnBeanByName();
                    if (StringUtils.hasText(beanByName) && getBean(beanByName) == null) {
                        missCondition = true;
                        break creteConfigBean;
                    }
                    Class<?> beanByType = info.conditionalOnBeanByType();
                    if (beanByType != Bean.class && getBean(beanByType) == null) {
                        missCondition = true;
                        break creteConfigBean;
                    }
                    String missBeanByName = info.conditionalOnMissBeanByName();
                    if (StringUtils.hasText(missBeanByName) && getBean(missBeanByName) != null) {
                        missCondition = true;
                        break creteConfigBean;
                    }
                    Class<?> missBeanByType = info.conditionalOnMissBeanByTyp();
                    if (missBeanByType != Bean.class && getBean(missBeanByType) != null) {
                        missCondition = true;
                    }
                }
                Method method = configDefinition.getBeanFactoryMethod();
                //??????????????????????????????????????????
                if (missCondition) {
                    if (LockProvider.initContainer.isInitialized()) return null;
                    throw new NoSuchBeanException("can't acquire config bean ?,because the factory method[?] do not meet the condition", definition.getBeanName(), method);
                }
                List<Object> args = getArgumentsFromBeanContainer(method);
                try {
                    bean = method.invoke(getBean(method.getDeclaringClass()), args.toArray());
                } catch (Exception e) {
                    log.error("invoke bean factory method error,because {}", e.getMessage());
                    throw new BeanException(e);
                }

                if (bean == null) {
                    log.warn("can't acquire config bean {},because the factory method's return value is null!", definition.getBeanName());
                }
                hasConstruct = true;
            }
            //??????PropertiesBeanDefinition
            if (definition instanceof PropertiesBeanDefinition propDefinition) {
                //????????????
                bean = injectByConstruct(definition.getBeanClass());
                //????????????
                if (!propertiesExtractor.injectProperties(propDefinition.getPrefix(), bean, Set.of(propDefinition.getExclude())) &&
                        propDefinition.isNonePutIfEmpty()) {
                    //???????????? ??? ?????????NonePutIfEmpty???true?????????bean????????????
                    bean = null;
                }

                hasConstruct = true;
            }
            if (!hasConstruct) {
                //????????????bean
                bean = injectByConstruct(definition.getBeanClass());
            }
            return bean;
        } catch (BeanException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeanException("create instance failed,because ?", e);
        }
    }

    private Object injectByConstruct(Class<?> beanType) {
        Constructor<?>[] constructors = beanType.getConstructors();

        Constructor<?> injectConstructor = null;
        //?????????????????????????????????????????????
        for (Constructor<?> constructor : constructors) {
            Inject inject = constructor.getAnnotation(Inject.class);
            if (inject != null) {
                if (injectConstructor != null) {
                    throw new BeanInitException("@Inject annotation can't use more than once in constructor");
                }
                injectConstructor = constructor;
            }
        }
        Object configBean;
        if (injectConstructor != null) {
            List<Object> constructorArgs = getArgumentsFromBeanContainer(injectConstructor);
            try {
                configBean = injectConstructor.newInstance(constructorArgs.toArray());
            } catch (Exception e) {
                throw new BeanInitException("can't init bean[type:?] because ?", beanType.getName(), e);
            }
        } else {
            //????????????
            for (Constructor<?> constructor : constructors) {
                if (injectConstructor != null) {
                    throw new BeanInitException("Multiple parameterized constructors detected!can't decide chose which one to inject bean,you can use @Inject annotation to decide one");
                }
                if (constructor.getParameters().length > 0) injectConstructor = constructor;
            }
            if (injectConstructor != null) {
                List<Object> constructorArgs = getArgumentsFromBeanContainer(injectConstructor);
                try {
                    configBean = injectConstructor.newInstance(constructorArgs.toArray());
                } catch (Exception e) {
                    throw new BeanInitException("can't init bean[type:?] because ?", beanType.getName(), e);
                }
            } else {
                try {
                    configBean = beanType.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new BeanInitException("can't init bean[type:?] because ?", beanType.getName(), e);
                }
            }
        }

        return configBean;
    }

    private void invokePostConstruct(Object bean, BeanDefinition definition) {
        List<BeanDefinition.DelayMethod> postConstruct = definition.getPostConstruct();
        if (postConstruct != null) {
            for (BeanDefinition.DelayMethod delayMethod : postConstruct) {
                Method method = delayMethod.relevantMethod();
                try {
                    method.invoke(bean);
                } catch (Throwable e) {
                    BeanException ex = new BeanException("invoke post-construct method[?] failed,because ?", method, e);
                    if (delayMethod.failFast()) {
                        ex.printStackTrace();
                        System.exit(0);
                    } else {
                        throw ex;
                    }
                }
            }
        }
        List<BeanDefinition.DelayMethod> injectMethods = definition.getInjectMethods();
        if (injectMethods != null) {
            for (BeanDefinition.DelayMethod delayMethod : injectMethods) {
                Method relevantMethod = delayMethod.relevantMethod();
                List<Object> args = getArgumentsFromBeanContainer(relevantMethod);
                try {
                    relevantMethod.invoke(bean, args.toArray());
                } catch (Throwable e) {
                    BeanException ex = new BeanException("invoke inject-methods[?] failed,because ?",relevantMethod, e);
                    if (delayMethod.failFast()){
                       ex.printStackTrace();
                       System.exit(0);
                   }else {
                       throw ex;
                   }
                }
            }
        }
    }



    /**
     * ?????????????????????????????????
     */
    private Object checkProxy(Object bean, BeanDefinition definition) {
        String beanClassName = definition.getBeanClass().getName();
        List<ProxyBeanDefinition.MataInterceptor> interceptor = new ArrayList<>();
        for (BeanDefinition value : beanDefinition.values()) {
            if (value instanceof ProxyBeanDefinition proxyBeanDefinition) {
                interceptor.addAll(proxyBeanDefinition.getConditionalIntercept(beanClassName));
            }
        }
        if (interceptor.isEmpty()) return bean;

        return new ProxyProcessor(bean, this, interceptor).getProxy();
    }


    public void setBean(Object bean) {
        setBean(convertName(bean.getClass().getSimpleName()), bean);
    }

    /**
     * ????????????????????????bean??????registerBean????????????????????????????????????bean?????????????????????
     *
     * @param beanName bean??????
     * @param bean     bean??????
     */
    public void setBean(String beanName, Object bean) {
        injectBean(bean);
        registerBean(beanName, bean);
    }

    private String convertName(String className) {
        return className.substring(0, 1).toLowerCase(Locale.ROOT) + className.substring(1);
    }

    @Override
    public boolean isSingleton(String name) {
        return beanDefinition.get(name).getType() == BeanType.SINGLETON;
    }

    @Override
    public boolean isPrototype(String name) {
        return beanDefinition.get(name).getType() == BeanType.PROTOTYPE;
    }

    @Override
    public Class<?> getType(String name) {
        return beanDefinition.get(name).getBeanClass();
    }

    protected abstract void beanDefinitionPostProcess(BeanDefinition beanTypeDefinition);

    protected abstract Object beanBeforeCreatedPostProcess(BeanDefinition definition);
}
