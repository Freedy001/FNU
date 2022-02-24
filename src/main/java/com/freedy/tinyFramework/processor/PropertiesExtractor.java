package com.freedy.tinyFramework.processor;

import com.freedy.Context;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.N;
import com.freedy.tinyFramework.annotation.prop.NonStrict;
import com.freedy.tinyFramework.annotation.prop.Skip;
import com.freedy.tinyFramework.exception.BeanException;
import com.freedy.tinyFramework.exception.BeanInitException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
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
    private final ThreadLocal<Integer> listModeIndexThreadLocal = new ThreadLocal<>();

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
            System.getProperties().forEach((k, v) -> map.put((String) k, (String) v));
            this.properties = map;
            log.info("配置文件路径: {}", propertyPath);
        } catch (IOException e) {
            throw new BeanInitException("load properties failed,because ?", e);
        }
    }

    public boolean injectProperties(String prefix, Object bean, Set<String> exclude) {
        return injectProperties(prefix, bean, exclude, true, false);
    }

    /**
     * 对bean进行属性注入
     *
     * @param prefix   属性前缀
     * @param bean     bean对象
     * @param force    严格模式
     * @param listMode bean是不是在集合中
     */
    public boolean injectProperties(String prefix, Object bean, Set<String> exclude, boolean force, boolean listMode) {
        Class<?> beanClass = bean.getClass();
        NonStrict nonStrict = beanClass.getAnnotation(NonStrict.class);
        Field[] allFields = beanClass.getDeclaredFields();
        int fieldCount = allFields.length;
        //获取哪个字段没有成功设置值
        Set<Integer> notSetIndexList = null;
        if (listMode) {
            notSetIndexList = new TreeSet<>();
        }
        //该对象是否被设置过值
        boolean hasSet = false;
        for (int i = 0; i < fieldCount; i++) {
            Field field = allFields[i];
            InjectProperties.Exclude exc = field.getAnnotation(InjectProperties.Exclude.class);
            if (field.getAnnotation(Inject.class) == null &&
                    field.getAnnotation(Skip.class) == null &&
                    !exclude.contains(field.getName()) &&
                    (exc == null || (!ReflectionUtils.isRegularType(field.getType()) && exc.exclude().length != 0))
            ) {
                String fieldName = field.getName();
                String propName = prefix + "." + StringUtils.convertConstantFieldToEntityField(fieldName);
                boolean injectState = injectValue(bean, field, propName, force && nonStrict == null, listMode);
                hasSet = injectState || hasSet;
                if (listMode && !injectState) {
                    notSetIndexList.add(i);
                }
            }
        }

        if (hasSet) {
            log.debug("inject prop on {} succeed!", listMode ? "collection<" + simple(bean) + ">[" + getIndex() + "]" : beanClass.getName());
        }

        //普通模式直接返回
        //notSetIndexList大小和allFields的大小相等表示所有字段设置失败，这时表示Collection构建完毕应该结束list mode
        if (!listMode || notSetIndexList.size() == fieldCount) return hasSet;
        //listMode 下进行严格模式检测
        for (Integer notSetIndex : notSetIndexList) {
            Field nullField = allFields[notSetIndex];
            NonStrict nf = nullField.getAnnotation(NonStrict.class);
            String fieldName = nullField.getName();
            String propName = prefix + "." + StringUtils.convertConstantFieldToEntityField(fieldName);
            if (force && nf == null)
                throw new InjectException("could not find a suitable property for a collection bean[type:?,index:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", name(bean), getIndex(), fieldName, propName, propertyPath);
        }

        return hasSet;
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
    private boolean injectValue(Object bean, Field field, String propName, boolean force, boolean listMode) {
        Class<?> type = field.getType();

        String propVal = properties.get(propName);
        Object fieldVal;
        createFieldVal:
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
                                map.put(ReflectionUtils.convertType(mapKey, _1stType[0]), ReflectionUtils.convertType(listMode ? getLMVal(v) : v, _2ndType[0]));
                            }
                        });
                    } else {
                        //没有声明泛型 默认string.string
                        map = new HashMap<>();
                        properties.forEach((key, v) -> {
                            if (key.startsWith(propName)) {
                                String mapKey = key.substring(propName.length() + 1);
                                map.put(mapKey, listMode ? getLMVal(v) : v);
                            }
                        });
                    }

                    //非空直接终止
                    if (!map.isEmpty()) {
                        fieldVal = map;
                        break createFieldVal;
                    }

                    NonStrict nonStrict = field.getAnnotation(NonStrict.class);
                    if (nonStrict != null) {
                        N[] value = nonStrict.defaultVal();
                        if (_1stType[0] != null && _2ndType[0] != null) {
                            for (N node : value) {
                                map.put(ReflectionUtils.convertType(node.k(), _1stType[0]), ReflectionUtils.convertType(node.v(), _2ndType[0]));
                            }
                        } else {
                            for (N node : value) {
                                map.put(node.k(), node.v());
                            }
                        }
                        fieldVal = map.isEmpty() ? null : map;
                    } else if (force) {
                        throw new InjectException("could not find a suitable property for bean[type:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", name(bean), field.getName(), propName, propertyPath);
                    } else {
                        fieldVal = null;
                    }

                } else if (ReflectionUtils.isSonInterface(type, "java.util.Collection")) {
                    if (listMode) {
                        throw new BeanInitException("The field of an object[type:?] in a collection (which in prop name is ?) cannot be of ? type", name(bean), propName, type.getName());
                    }
                    //生成list泛型为对象时的list对象
                    if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                        Type[] genericType = parameterizedType.getActualTypeArguments();
                        Class<?> listType = (Class<?>) genericType[0];
                        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                        if (!ReflectionUtils.isRegularType(listType)) {
                            NonStrict nonStrict = field.getAnnotation(NonStrict.class);
                            Collection<Object> listContainer = ReflectionUtils.buildCollectionByType(rawType);

                            InjectProperties.Exclude exclude = field.getAnnotation(InjectProperties.Exclude.class);

                            //构建collection
                            Object filedObj = listType.getConstructor().newInstance();
                            while (injectProperties(
                                    propName, filedObj,
                                    Set.of(exclude == null ? new String[]{} : exclude.exclude()),
                                    false, true
                            )) {
                                listContainer.add(filedObj);
                                filedObj = listType.getConstructor().newInstance();
                                addIndex();
                            }
                            clearIndex();

                            //非空直接终止
                            if (!listContainer.isEmpty()) {
                                fieldVal = listContainer;
                                break createFieldVal;
                            }

                            //配置文件中找不到相应的属性 使用默认值重新构建
                            if (nonStrict != null) {
                                if (!setDefaultMapVal(propName, nonStrict)) {
                                    //没指定默认值 直接返回null
                                    fieldVal = null;
                                    break createFieldVal;
                                }

                                //设置成功，重新尝试构建集合
                                while (injectProperties(
                                        propName, filedObj,
                                        Set.of(exclude == null ? new String[]{} : exclude.exclude()),
                                        false, true
                                )) {
                                    listContainer.add(filedObj);
                                    filedObj = listType.getConstructor().newInstance();
                                    addIndex();
                                }
                                clearIndex();
                            } else if (force) {
                                throw new InjectException("could not find a suitable property for Collection<?>'s filed[filedName:?,PropName:?.<ANY(.*&?)>] in properties file[path:?]", simple(bean), field.getName(), propName, propertyPath);
                            }

                            fieldVal = listContainer.isEmpty() ? null : listContainer;
                            break createFieldVal;
                        }
                    }
                    //无泛型，进行严格检测
                    checkForce(bean, field, propName, force);
                    fieldVal = null;
                } else if (ReflectionUtils.isRegularType(type) || type.isArray()) {
                    checkForce(bean, field, propName, force);
                    fieldVal = null;
                } else {
                    // 普通obj类型
                    NonStrict nonStrict = field.getAnnotation(NonStrict.class);
                    Object filedObj = type.getConstructor().newInstance();

                    InjectProperties.Exclude exclude = field.getAnnotation(InjectProperties.Exclude.class);
                    boolean hasSetVal;
                    try {
                        hasSetVal = injectProperties(
                                propName, filedObj,
                                Set.of(exclude == null ? new String[]{} : exclude.exclude()),
                                force && nonStrict == null, listMode
                        );
                    } catch (BeanException e) {
                        throw new InjectException("Failed to create none-basic field[name:?,type:?] for bean[type:?],because ?", field.getName(), name(field.getType()), name(bean), e);
                    }

                    //设置成功直接终止
                    if (hasSetVal) {
                        fieldVal = filedObj;
                        break createFieldVal;
                    }

                    //设置失败，尝试默认值
                    if (nonStrict != null) {
                        if (!setDefaultMapVal(propName, nonStrict)) {
                            fieldVal = null;
                            break createFieldVal;
                        }
                        //尝试默认值
                        hasSetVal = injectProperties(
                                propName, filedObj,
                                Set.of(exclude == null ? new String[]{} : exclude.exclude()),
                                false, listMode
                        );
                    } else if (force) {
                        throw new InjectException("could not find a suitable property for bean[type:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", name(bean), field.getName(), propName, propertyPath);
                    }


                    fieldVal = hasSetVal ? filedObj : null;
                }
            } catch (BeanException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeanInitException("can not create bean[name:?]'s field[name:?,type:?],because ?", name(bean), field.getName(), type.getName(), e);
            }
        } else {  //list 或 基本类型
            if (ReflectionUtils.isRegularType(type)) {
                //普通类型
                if (listMode) {
                    String lmVal = getLMVal(propVal);
                    fieldVal = ReflectionUtils.convertType(lmVal, type);
                } else {
                    fieldVal = ReflectionUtils.convertType(propVal, type);
                }
            } else {
                //Collection类型
                if (ReflectionUtils.isSonInterface(type, "java.util.Collection")) {
                    if (listMode) {
                        String lmVal = getLMVal(propVal);
                        if (lmVal == null) {
                            fieldVal = null;
                        } else {
                            fieldVal = ReflectionUtils.buildCollectionByFiledAndValue(field, lmVal.split("\\|"));
                        }
                    } else {
                        fieldVal = ReflectionUtils.buildCollectionByFiledAndValue(field, propVal.split(","));
                    }
                } else if (type.isArray()) {
                    //arr类型
                    if (listMode) {
                        String lmVal = getLMVal(propVal);
                        if (lmVal == null) {
                            fieldVal = null;
                        } else {
                            fieldVal = ReflectionUtils.buildArrByArrFieldAndVal(type, lmVal.split("\\|"));
                        }
                    } else {
                        fieldVal = ReflectionUtils.buildArrByArrFieldAndVal(type, propVal.split(","));
                    }
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
            } else {
                if (!listMode)
                    log.warn("could not find a suitable property for bean[className:{}]'s filed[filedName:{},PropName:{}] in properties file[path:{}]", name(bean), field.getName(), propName, propertyPath);
            }
        } catch (Exception e) {
            throw new BeanInitException("set value failed,because ?", e);
        }
        return false;
    }

    /**
     * 对properties进行默认值的填充
     *
     * @return true设置成功 false 设置失败
     */
    private boolean setDefaultMapVal(String propName, NonStrict nonStrict) {
        N[] listVal = nonStrict.defaultVal();
        if (listVal.length == 0) return false;
        for (N node : listVal) {
            properties.put(propName + "." + node.k(), node.v());
        }
        return true;
    }

    /**
     * 如果从properties中获取的值为空则需要调用此方法来检测是否需要强制抛异常
     */
    private void checkForce(Object bean, Field field, String propName, boolean force) {
        NonStrict nonStrict = field.getAnnotation(NonStrict.class);
        if (nonStrict == null && force) {
            //基本类型取值为空抛异常
            throw new InjectException("could not find a suitable property for bean[className:?]'s filed[filedName:?,PropName:?] in properties file[path:?]", name(bean), field.getName(), propName, propertyPath);
        }
    }

    /**
     * 获取bean的全类名
     */
    private String name(Object bean) {
        return bean.getClass().getName();
    }

    private String simple(Object bean) {
        return bean.getClass().getSimpleName();
    }

    private int getIndex() {
        return listModeIndexThreadLocal.get();
    }

    private String getLMVal(String val) {
        String[] split = val.split(",");
        Integer index = listModeIndexThreadLocal.get();
        if (index == null) {
            listModeIndexThreadLocal.set(0);
            index = 0;
        }
        if (index < split.length) {
            return split[index];
        }
        return null;
    }

    private void addIndex() {
        Integer index = listModeIndexThreadLocal.get();
        if (index == null) {
            throw new IllegalArgumentException("you should call getIndex() method first!");
        }
        listModeIndexThreadLocal.set(index + 1);
    }

    private void clearIndex() {
        listModeIndexThreadLocal.remove();
    }
}
