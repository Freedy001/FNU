package com.freedy.tinyFramework.processor;

import com.freedy.Context;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.prop.Node;
import com.freedy.tinyFramework.annotation.prop.NoneForce;
import com.freedy.tinyFramework.exception.BeanException;
import com.freedy.tinyFramework.exception.BeanInitException;
import com.freedy.tinyFramework.exception.InjectException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/5 15:57
 */
@SuppressWarnings("unchecked")
@Slf4j
public class PropertiesExtractor {


    private final Map<String, String> properties;
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
            HashMap<String, String> map = new HashMap<>();
            properties.forEach((k, v) -> map.put((String) k, (String) v));
            this.properties = map;
            log.info("配置文件路径: {}", propertyPath);
        } catch (IOException e) {
            throw new BeanInitException("load properties failed,because ?", e);
        }
    }

    /**
     * 对bean进行属性注入
     *
     * @param prefix 属性前缀
     * @param bean   bean对象
     * @param force  严格模式
     */
    public void injectProperties(String prefix, Object bean, boolean force) {
        Class<?> beanClass = bean.getClass();
        NoneForce noneForce = beanClass.getAnnotation(NoneForce.class);
        boolean hasSet = false;
        for (Field field : beanClass.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) == null) {
                String fieldName = field.getName();
                String propName = prefix + "." + StringUtils.convertConstantFieldToEntityField(fieldName);
                hasSet = hasSet || injectValue(bean, field, propName, force && noneForce == null);
            }
        }
        if (hasSet) {
            log.debug("inject prop on {} succeed!", beanClass.getName());
        }
    }

    /**
     * 对bean的具体字段进行注入
     *
     * @param bean     bean对象
     * @param field    bean字段
     * @param propName 属性前缀
     * @param force    严格模式
     * @return true注入成功 false表示注入失败
     */
    private boolean injectValue(Object bean, Field field, String propName, boolean force) {
        Class<?> type = field.getType();

        String propVal = properties.get(propName);
        Object fieldVal;
        if (propVal == null) {
            //取值为空 可能为map或者对象类型
            try {
                //map类型
                if (ReflectionUtils.isSonInterface(type, "java.util.Map")) {
                    Map<Object, Object> map;
                    Class<?>[] _1stType = new Class[1];
                    Class<?>[] _2ndType = new Class[1];
                    if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                        Type[] genericType = parameterizedType.getActualTypeArguments();
                        _1stType[0] = (Class<?>) genericType[0];
                        _2ndType[0] = (Class<?>) genericType[1];
                        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                        if (!rawType.isInterface()) {
                            //非interface
                            map = (Map<Object, Object>) rawType.getConstructor().newInstance();
                        } else {
                            map = new HashMap<>();
                        }
                        properties.forEach((key, v) -> {
                            if (key.startsWith(propName)) {
                                String mapKey = key.substring(propName.length() + 1);
                                map.put(ReflectionUtils.convertType(mapKey, _1stType[0]), ReflectionUtils.convertType(v, _2ndType[0]));
                            }
                        });
                    } else {
                        //没有声明泛型 默认string.string
                        map = new HashMap<>();
                        properties.forEach((key, v) -> {
                            if (key.startsWith(propName)) {
                                String mapKey = key.substring(propName.length() + 1);
                                map.put(mapKey, v);
                            }
                        });
                    }
                    if (map.isEmpty()) {
                        NoneForce noneForce = field.getAnnotation(NoneForce.class);
                        if (noneForce != null) {
                            Node[] value = noneForce.mapTypeValIfNone();
                            if (_1stType[0] != null && _2ndType[0] != null) {
                                for (Node node : value) {
                                    map.put(ReflectionUtils.convertType(node.key(), _1stType[0]), ReflectionUtils.convertType(node.val(), _2ndType[0]));
                                }
                            } else {
                                for (Node node : value) {
                                    map.put(node.key(), node.val());
                                }
                            }
                            fieldVal = map.isEmpty() ? null : map;
                        } else if (force) {
                            throw new InjectException("could not find a suitable property for bean[className:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", bean.getClass().getName(), field.getName(), propName, propertyPath);
                        } else {
                            fieldVal = null;
                        }
                    } else {
                        fieldVal = map;
                    }
                } else if (ReflectionUtils.isSonInterface(type, "java.util.Collection")) {
                    fieldVal = injectCollectionTypeVal(bean, field, propName, null, force);
                } else if (ReflectionUtils.isBasicType(type)) {
                    NoneForce noneForce = field.getAnnotation(NoneForce.class);
                    if (noneForce != null) {
                        String value = noneForce.normalTypeValIfNone();
                        fieldVal = StringUtils.hasText(value) ? ReflectionUtils.convertType(value, type) : null;
                    } else if (force) {
                        //基本类型取值为空抛异常
                        throw new InjectException("could not find a suitable property for bean[className:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", bean.getClass().getName(), field.getName(), propName, propertyPath);
                    } else {
                        fieldVal = null;
                    }
                } else {
                    // 普通obj类型
                    NoneForce noneForce = field.getAnnotation(NoneForce.class);
                    Object filedObj = type.getConstructor().newInstance();
                    injectProperties(propName, filedObj, force && noneForce == null);
                    fieldVal = filedObj;
                }
            } catch (BeanException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeanInitException("can not create bean[name:?]'s field[name:?,type:?],because ?", bean.getClass().getName(), field.getName(), type.getName(), e);
            }
        } else {  //list 或 基本类型
            if (ReflectionUtils.isBasicType(type)) {
                //普通类型
                fieldVal = ReflectionUtils.convertType(propVal, type);
            } else {
                //Collection类型
                if (ReflectionUtils.isSonInterface(type, "java.util.Collection")) {
                    fieldVal = injectCollectionTypeVal(bean, field, propName, propVal, force);
                } else {
                    throw new BeanInitException("not supported type:?", type.getName());
                }
            }
        }

        //set value
        try {
            if (fieldVal != null) {
                field.setAccessible(true);
                field.set(bean, fieldVal);
                return true;
            }
        } catch (Exception e) {
            throw new BeanInitException("set value failed,because ?", e);
        }
        return false;
    }

    private Object injectCollectionTypeVal(Object bean, Field field, String propName, String propVal, boolean force) {
        Object fieldVal;
        try {
            Collection<Object> collection = ReflectionUtils.buildCollectionByFiledAndValue(field, propVal == null ? null : propVal.split(","));

            if (collection.isEmpty()) {
                NoneForce noneForce = field.getAnnotation(NoneForce.class);
                if (noneForce != null) {
                    String value = noneForce.normalTypeValIfNone();
                    if (StringUtils.hasText(value))
                        collection.addAll(Arrays.asList(value.split(",")));
                    else
                        collection = null;
                } else if (force) {
                    throw new InjectException("could not find a suitable property for bean[className:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", bean.getClass().getName(), field.getName(), propName, propertyPath);
                } else {
                    collection = null;
                }
            }
            fieldVal = collection;
        } catch (Throwable ex) {
            throw new BeanInitException("can not create bean[name:?]'s field[name:?,type:?],because ?", bean.getClass().getName(), field.getName(), field.getType().getName(), ex);
        }
        return fieldVal;
    }


}
