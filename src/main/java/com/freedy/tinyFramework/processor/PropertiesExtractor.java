package com.freedy.tinyFramework.processor;

import com.freedy.Context;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.exception.BeanInitException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/5 15:57
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PropertiesExtractor {


    private final Properties properties;
    private final String propertyPath;

    public PropertiesExtractor() {
        Properties properties = new Properties();
        try {
            File file = new File("./conf.properties");
            if (file.exists()) {
                properties.load(new FileInputStream(file));
                propertyPath = file.getAbsolutePath();
            } else {
                properties.load(Context.class.getClassLoader().getResourceAsStream("conf.properties"));
                propertyPath = Objects.requireNonNull(Context.class.getClassLoader().getResource("conf.properties")).getPath();
            }
            this.properties = properties;
            log.info("配置文件路径: {}", propertyPath);
        } catch (IOException e) {
            throw new BeanInitException("load properties failed,because ?", e);
        }
    }

    public void injectProperties(String prefix, Object bean) {
        Class<?> beanClass = bean.getClass();
        for (Field field : beanClass.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) == null) {
                String fieldName = field.getName();
                String propName = prefix + "." + StringUtils.convertConstantFieldToEntityField(fieldName);
                injectValue(bean, field, propName);
            }
        }

    }

    private void injectValue(Object bean, Field field, String propName) {
        Class<?> type = field.getType();

        String propVal = (String) properties.get(propName);
        if (propVal == null) {
            throw new BeanInitException("could not find property[name:?] in properties file[path:?]", field.getName(), propertyPath);
        }
        Object fieldVal = null;
        try {
            //普通类型
            fieldVal = ReflectionUtils.convertType(propVal, type);
        } catch (UnsupportedOperationException e) {
            //复杂类型
            try {
                if (ReflectionUtils.isSonInterface(type, "java.util.Map")) {
                    //map
                    Map<Object, Object> map;
                    if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                        Type[] genericType = parameterizedType.getActualTypeArguments();
                        Class<?> _1stType = (Class<?>) genericType[0];
                        Class<?> _2ndType = (Class<?>) genericType[0];
                        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                        if (!rawType.isInterface()) {
                            //非interface
                            map = (Map<Object, Object>) rawType.getConstructor().newInstance();
                        } else {
                            map = new HashMap<>();
                        }
                        properties.forEach((k, v) -> {
                            if (k instanceof String key) {
                                if (key.startsWith(propName)) {
                                    String mapKey = key.substring(propName.length() + 1);
                                    map.put(ReflectionUtils.convertType(mapKey, _1stType), ReflectionUtils.convertType(v, _2ndType));
                                } else {
                                    throw new IllegalArgumentException("properties changed");
                                }
                            }
                        });
                    } else {
                        //没有声明泛型 默认string.string
                        map = new HashMap<>();
                        properties.forEach((k, v) -> {
                            if (k instanceof String key) {
                                if (key.startsWith(propName)) {
                                    String mapKey = key.substring(propName.length() + 1);
                                    map.put(mapKey, v);
                                } else {
                                    throw new IllegalArgumentException("properties changed");
                                }
                            }
                        });
                    }
                    if (!map.isEmpty()) fieldVal = map;
                } else if (ReflectionUtils.isSonInterface(type, "java.util.Collection")) {
                    // collection
                    Collection<Object> collection;
                    if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                        Type[] genericType = parameterizedType.getActualTypeArguments();
                        Class<?> listType = (Class<?>) genericType[0];
                        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                        if (!rawType.isInterface()) {
                            //非interface
                            collection = (Collection<Object>) rawType.getConstructor().newInstance();
                        } else {
                            collection = propCollection(rawType);
                        }
                        for (String s : propVal.split(",")) {
                            collection.add(ReflectionUtils.convertType(s, listType));
                        }
                    } else {
                        //没有声明泛型 默认string
                        collection = propCollection(type);
                        collection.addAll(Arrays.asList(propVal.split(",")));
                    }
                    fieldVal = collection;
                } else {
                    // 普通obj类型
                    Object filedObj = type.getConstructor().newInstance();
                    injectProperties(propName,filedObj);
                    fieldVal = filedObj;
                }

            } catch (Throwable ex) {
                throw new BeanInitException("Object field create failed,because ?", ex);
            }


        }

        //set value
        try {
            field.setAccessible(true);
            field.set(bean, fieldVal);
        } catch (Exception e) {
            throw new BeanInitException("set value failed,because ?", e);
        }
    }


    interface PropCollection extends List<Object>, Set<Object>, Queue<Object> {
        @Override
        Spliterator<Object> spliterator();
    }

    public PropCollection propCollection(Class<?> interfaceType) {
        switch (interfaceType.getName()) {
            case "java.util.List" -> {
                List<Object> list = new ArrayList<>();
                return (PropCollection) Proxy.newProxyInstance(PropCollection.class.getClassLoader(), new Class[]{PropCollection.class}, (proxy, method, args) -> method.invoke(list, args));
            }
            case "java.util.Set" -> {
                Set<Object> set = new HashSet<>();
                return (PropCollection) Proxy.newProxyInstance(PropCollection.class.getClassLoader(), new Class[]{PropCollection.class}, (proxy, method, args) -> method.invoke(set, args));
            }
            case "java.util.Queue" -> {
                Queue<Object> queue = new ArrayDeque<>();
                return (PropCollection) Proxy.newProxyInstance(PropCollection.class.getClassLoader(), new Class[]{PropCollection.class}, (proxy, method, args) -> method.invoke(queue, args));
            }
            default -> throw new BeanInitException("unsupported type for type ?,please change a supported(List,Set,Queue) type");
        }
    }

}
