package com.freedy.tinyFramework.utils;


import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.freedy.tinyFramework.utils.DateUtils.getDate;
import static com.freedy.tinyFramework.utils.DateUtils.getDateStrPattern;


/**
 * 反射工具类.
 * @author Freedy
 * @date 2021/12/2 16:01
 */
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReflectionUtils {


    /**
     * 通过getter方法获取指定字段名的值
     *
     * @param object    需要被获取的对象
     * @param fieldName 字段值
     * @return 对应字段的值 没有则返回null
     */
    public static Object getter(Object object, String fieldName) {
        Class<?> objectClass = object.getClass();
        try {
            String getterMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            log.debug("invoke " + objectClass.getSimpleName() + "'s getter method===> {}", getterMethodName + "()");
            return objectClass.getMethod(getterMethodName).invoke(object);
        } catch (NoSuchMethodException e) {
            String getterMethodName = "get" + fieldName;
            log.warn("getter method invoke fail!change getter name to {}", getterMethodName);
            try {
                return objectClass.getMethod(getterMethodName).invoke(object);
            } catch (Exception ex) {
                log.error(e.getClass().getSimpleName() + "===>" + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error(e.getClass().getSimpleName() + "===>" + e.getMessage());
            return null;
        }
    }


    /**
     * 调用setter方法对相应字段进行设置
     *
     * @param object     需要被设置字段的对象
     * @param fieldName  字段名
     * @param fieldValue 需要设置的值
     * @return 是否设置成功
     */
    public static boolean setter(Object object, String fieldName, Object fieldValue) {
        Class<?> objectClass = object.getClass();
        try {
            String setterMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            log.debug("invoke " + objectClass.getSimpleName() + "'s setter method===> {}", setterMethodName + "(" + fieldValue + ")");
            objectClass.getMethod(setterMethodName, getFieldRecursion(object.getClass(), fieldName).getType()).invoke(object, fieldValue);
            return true;
        } catch (NoSuchMethodException e) {
            String setterMethodName = "set" + fieldName;
            log.warn("setter method invoke fail!change setter name to {}", setterMethodName);
            try {
                log.debug("invoke " + objectClass.getSimpleName() + "'s setter method===> {}", setterMethodName + "(" + fieldValue + ")");
                objectClass.getMethod(setterMethodName, getFieldRecursion(object.getClass(), fieldName).getType()).invoke(object, fieldValue);
                return true;
            } catch (Exception ex) {
                log.error(e.getClass().getSimpleName() + "===>" + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error(e.getClass().getSimpleName() + "===>" + e.getMessage());
            return false;
        }
    }

    /**
     * @param object     需要被设置字段的对象
     * @param fieldName  字段名
     * @param fieldValue 需要设置的值
     * @return 是否设置成功
     */
    public static boolean setter(Object object, String fieldName, String fieldValue) {
        Class<?> objectClass = object.getClass();
        Field field = getFieldRecursion(object.getClass(), fieldName);
        if (field == null) return false;
        Class<?> fieldType = field.getType();
        try {
            String setterMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            if (!setObjValue(object, fieldValue, objectClass, fieldType, setterMethodName)) {
                throw new IllegalArgumentException("can not find type handler!");
            }
            return true;
        } catch (Exception e) {
            log.error(e.getClass().getSimpleName() + "===>" + e.getMessage());
            return false;
        }
    }

    private static boolean setObjValue(Object object, String fieldValue, Class<?> objectClass, Class<?> fieldType, String setterMethodName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        log.debug("invoke " + objectClass.getSimpleName() + "'s setter method===> {}", setterMethodName + "(" + fieldValue + ")");
        switch (fieldType.getSimpleName()) {
            case "String" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, fieldValue);
            case "Date" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, getDate(fieldValue));
            case "LocalDateTime" -> objectClass.getMethod(setterMethodName, fieldType)
                    .invoke(object, LocalDateTime.parse(fieldValue,
                                    DateTimeFormatter.ofPattern(getDateStrPattern(fieldValue))
                            )
                    );
            case "Integer", "int" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, Integer.parseInt(fieldValue));
            case "Long", "long" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, Long.parseLong(fieldValue));
            case "Double", "double" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, Double.parseDouble(fieldValue));
            case "BigDecimal" -> objectClass.getMethod(setterMethodName, fieldType).invoke(object, new BigDecimal(fieldValue));
            default -> {
                return false;
            }
        }
        return true;
    }

    public static <T extends Annotation> List<Field> getFieldsByAnnotationValue(Class<?> clazz, Class<T> annotationClazz, String regex) {
        try {
            List<Field> list = new ArrayList<>();
            Method valueMethod = annotationClazz.getDeclaredMethod("value");
            for (Field field : getFieldsRecursion(clazz)) {
                T annotation = field.getAnnotation(annotationClazz);
                if (annotation == null) continue;
                Object o = valueMethod.invoke(annotation);
                String realValue;
                if (o instanceof String) {
                    realValue = (String) o;
                } else if (o instanceof String[]) {
                    realValue = ((String[]) o)[0];
                } else throw new UnsupportedOperationException("仅支持注解类型为String的操作");
                if (realValue.matches(regex)) {
                    list.add(field);
                }
            }
            return list.size() == 0 ? null : list;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("当前注解不包含value字段");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取包括父类在内的所有Field对象
     */
    public static List<Field> getFieldsRecursion(Class clazz) {
        List<Field> list = new ArrayList<>();
        for (Class aClass : getClassRecursion(clazz)) {
            list.addAll(Arrays.asList(aClass.getDeclaredFields()));
        }
        return list;
    }

    /**
     * 获取包括父类在内的指定Field对象
     */
    public static Field getFieldRecursion(Class clazz, String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }
        if (field == null) {
            Class superclass = clazz.getSuperclass();
            while (superclass != null && field == null) {
                try {
                    field = superclass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
                superclass = superclass.getSuperclass();
            }
        }
        return field;
    }

    /**
     * 通过递归获取注解
     *
     * @param clazz           要进行扫描的类
     * @param annotationClass 注解类型
     * @param fieldName       注解类型
     */
    public static <T extends Annotation, A> List<T> getAnnotationRecursion(Class<A> clazz, Class<T> annotationClass, ElementType... fieldName) {
        List<T> annotationList = new ArrayList<>();
        Set<Class<?>> superclass = getClassRecursion(clazz);
        if (fieldName != null) {
            Arrays.stream(fieldName).distinct().toList().forEach(type -> {
                switch (type) {
                    case TYPE:
                        for (Class aClass : superclass) {
                            T annotation = (T) aClass.getAnnotation(annotationClass);
                            if (annotation != null)
                                annotationList.add(annotation);
                        }
                        break;
                    case METHOD:
                        for (Class aClass : superclass) {
                            for (Method method : aClass.getDeclaredMethods()) {
                                T annotation = method.getAnnotation(annotationClass);
                                if (annotation != null)
                                    annotationList.add(annotation);
                            }
                        }
                        break;
                    case FIELD:
                        for (Class aClass : superclass) {
                            for (Field field : aClass.getDeclaredFields()) {
                                T annotation = field.getAnnotation(annotationClass);
                                if (annotation != null)
                                    annotationList.add(annotation);
                            }
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("此方法仅支持type、field、method类型");
                }
            });
        } else {
            for (Class aClass : superclass) {
                T fieldAnnotation = (T) aClass.getAnnotation(annotationClass);
                if (fieldAnnotation != null)
                    annotationList.add(fieldAnnotation);
                for (Method method : aClass.getDeclaredMethods()) {
                    T annotation = method.getAnnotation(annotationClass);
                    if (annotation != null)
                        annotationList.add(annotation);
                }
                for (Field field : aClass.getDeclaredFields()) {
                    T annotation = field.getAnnotation(annotationClass);
                    if (annotation != null)
                        annotationList.add(annotation);
                }
            }
        }
        return annotationList;
    }

    /**
     * 获取该类及所有他的父类
     */
    public static Set<Class<?>> getClassRecursion(Class<?> clazz) {
        Set<Class<?>> list = new HashSet<>();
        list.add(clazz);
        Class superclass = clazz.getSuperclass();
        while (superclass != null) {
            list.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return list;
    }

    /**
     * 获取该类及所有他的父类
     */
    public static Set<Class<?>> getClassRecursion(Object o) {
        return getClassRecursion(o.getClass());
    }

    /**
     * 判断某个类是不是指定接口的实现类
     * @param clazz                 需要被判断的类
     * @param fatherInterfaceName   用于判断是否是类的实现接口
     * @return                      是不是指定接口的实现类
     */
    public static boolean isSonInterface(Class<?> clazz, String... fatherInterfaceName) {
        Stack<Class<?>> stack = new Stack<>();
        stack.push(clazz);
        for (Class<?> aClass : clazz.getInterfaces()) {
            stack.push(aClass);
        }
        while (!stack.isEmpty()) {
            Class<?> pop = stack.pop();
            for (String s : fatherInterfaceName) {
                if (pop.getName().equals(s)) return true;
            }
            for (Class<?> popInterface : pop.getInterfaces()) stack.push(popInterface);
        }
        return false;
    }

    public static  <T> T convertType(Object strValue,Class<T> type){
        if (strValue==null) return null;
        Object returnValue;
        switch (type.getSimpleName()) {
            case "Integer", "int" -> returnValue=Integer.parseInt(strValue.toString());
            case "Long", "long" -> returnValue=Long.parseLong(strValue.toString());
            case "Double", "double" -> returnValue=Double.parseDouble(strValue.toString());
            case "Float", "float" -> returnValue=Float.parseFloat(strValue.toString());
            case "Boolean", "boolean" -> returnValue=Boolean.parseBoolean(strValue.toString());
            case "short", "Short" -> returnValue=Short.parseShort(strValue.toString());
            case "Byte", "byte" ->returnValue=Byte.parseByte(strValue.toString());
            case "Character", "char" -> returnValue=strValue.toString().charAt(0);
            case "String" -> returnValue=strValue.toString();
            default -> throw new UnsupportedOperationException("unsupported current type "+type.getName());
        }
        return type.cast(returnValue);
    }

}

