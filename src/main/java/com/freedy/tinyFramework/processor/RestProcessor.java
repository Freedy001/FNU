package com.freedy.tinyFramework.processor;

import com.alibaba.fastjson.JSON;
import com.freedy.manage.Response;
import com.freedy.manage.exception.ErrorMsgException;
import com.freedy.tinyFramework.annotation.mvc.*;
import com.freedy.tinyFramework.utils.DateUtils;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @author Freedy
 * @date 2021/11/29 9:38
 */
@Slf4j
@ChannelHandler.Sharable
public class RestProcessor extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Map<String, ControllerMethod> requestHandleMapping = new HashMap<>();
    private final Map<String, Object> innerObject = new ConcurrentHashMap<>();

    public void registerInnerObj(@NonNull Object innerObj) {
        innerObject.put(innerObj.getClass().getName(), innerObj);
    }



    @SneakyThrows
    public void registerController(Object controllerBean) {
        Class<?> controllerClass = controllerBean.getClass();
        REST REST = controllerClass.getAnnotation(REST.class);
        if (REST == null) return;
        String baseUrl = getUrl(REST.value());

        for (Method method : controllerClass.getDeclaredMethods()) {
            Get get = method.getAnnotation(Get.class);
            Post post = method.getAnnotation(Post.class);
            if (get != null) {
                String subUrl = getUrl(get.value());
                if (subUrl.equals("")) {
                    subUrl = "/" + method.getName();
                }
                requestHandleMapping.put(baseUrl + subUrl, new ControllerMethod("get", method, controllerBean));
                log.debug("fond one get request mapping: {}", baseUrl + subUrl);
            }
            if (post != null) {
                String subUrl = getUrl(post.value());
                if (subUrl.equals("")) {
                    subUrl = "/" + method.getName();
                }
                ControllerMethod controllerMethod = requestHandleMapping.get(baseUrl + subUrl);
                if (controllerMethod != null) {
                    controllerMethod.httpMethodName = controllerMethod.httpMethodName + "|post";
                } else {
                    requestHandleMapping.put(baseUrl + subUrl, new ControllerMethod("post", method, controllerBean));
                }
                log.debug("fond one post request mapping: {}", baseUrl + subUrl);
            }
        }
    }


    @ToString
    private static class ControllerMethod {
        @Getter
        private String httpMethodName;
        @Getter
        private final ArgumentInfo[] argumentInfos;
        @Getter
        private final Class<?> returnType;
        @Getter
        private final Method invokeMethod;
        private final Object instance;
        private final ThreadLocal<List<Object>> args = new ThreadLocal<>();
        private final Set<String> baseType = new HashSet<>(List.of("int", "Integer", "long", "Long", "boolean", "Boolean", "byte", "Byte", "short", "Short", "char", "Char", "double", "Double", "float", "Float"));


        ControllerMethod(String httpMethod, Method method, Object instance) {
            ArgumentInfo[] argumentInfos = new ArgumentInfo[method.getParameterCount()];
            int index = 0;
            boolean firstBody = false;
            for (Parameter parameter : method.getParameters()) {
                Param param = parameter.getAnnotation(Param.class);
                Body body = parameter.getAnnotation(Body.class);
                if (param != null && body != null)
                    throw new IllegalArgumentException("annotation Param and Body shouldn't appear together");
                if (body != null) {
                    if (firstBody)
                        throw new IllegalArgumentException("annotation body is allowed only once in method " + method.getName());
                    firstBody = true;
                    argumentInfos[index++] = new ArgumentInfo(parameter.getName(), body, parameter.getType());
                } else {
                    Class<?> type = parameter.getType();
                    if (!baseType.contains(type.getSimpleName())) {
                        //非基本类型
                        for (Field field : type.getDeclaredFields()) {
                            if (ReflectionUtils.isSonInterface(field.getType(), "java.util.Collection", "java.util.Map")) {
                                //集合类型
                                throw new UnsupportedOperationException("request param doesn't support Collection type or Map type");
                            }
                        }

                    }
                    argumentInfos[index++] = new ArgumentInfo(parameter.getName(), param, type);
                }
            }
            method.setAccessible(true);
            this.httpMethodName = httpMethod;
            this.argumentInfos = argumentInfos;
            this.returnType = method.getReturnType();
            this.invokeMethod = method;
            this.instance = instance;
        }

        public Object invoke() {
            try {
                Object[] argArray = args.get().toArray();
                args.remove();
                return invokeMethod.invoke(instance, argArray);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                throw new ErrorMsgException(cause == null ? e : cause);
            }
        }

        public void setArgument(Object arg) {
            List<Object> list = args.get();
            if (list != null)
                list.add(arg);
            else
                args.set(new ArrayList<>(List.of(arg)));
        }


        private record ArgumentInfo(String name, Annotation type, Class<?> clazz) {
        }
    }


    private static String getUrl(String rawUrl) {
        rawUrl = rawUrl.trim();
        if (!rawUrl.startsWith("/")) {
            rawUrl = "/" + rawUrl;
        }
        if (rawUrl.endsWith("/")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
        }
        return URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        HttpUrl httpUrl = new HttpUrl(req.uri());
        ControllerMethod method = requestHandleMapping.get(httpUrl.getUrl());
        if (method == null) {
            log.error("the request url[{}] is not exist", httpUrl.url);
            responseError(ctx, 404, "the url your request is not exist");
            return;
        }
        if (!method.getHttpMethodName().contains(req.method().name().toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("The request which url is " + httpUrl.getUrl() + " should be " + method.getHttpMethodName() + " method but it is actually " + req.method().name().toLowerCase(Locale.ROOT) + " method");

        fillArguments(req, httpUrl, method);

        Object invoke = method.invoke();


        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONNECTION, "keep-alive");
        response.headers().set(SERVER, "FNU power by netty");
        response.headers().set(DATE, new Date());
        response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, "*");
        response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "*");
        byte[] bytes = JSON.toJSONBytes(invoke);
        response.headers().set(CONTENT_LENGTH, bytes.length);
        response.content().writeBytes(bytes);
        ctx.writeAndFlush(response);
    }

    private void fillArguments(FullHttpRequest req, HttpUrl httpUrl, ControllerMethod method) throws Exception {
        for (ControllerMethod.ArgumentInfo argumentInfo : method.getArgumentInfos()) {
            Class<?> paramClazz = argumentInfo.clazz();
            //请求体
            if (argumentInfo.type() != null && argumentInfo.type().annotationType().getName().equals("com.freedy.manage.tinyFramework.annotation.Body")) {
                Object arg = JSON.parseObject(req.content().toString(StandardCharsets.UTF_8), paramClazz);
                method.setArgument(arg);
                continue;
            }

            //内部对象
            if (paramClazz.getName().equals("io.netty.handler.codec.http.FullHttpRequest")) {
                method.setArgument(req);
                continue;
            }
            Object innerObj = innerObject.get(paramClazz.getName());
            if (innerObj != null) {
                method.setArgument(innerObj);
                continue;
            }

            //请求参数request param
            boolean require = false;
            String parameterName = argumentInfo.name();
            if (argumentInfo.type instanceof Param p) {
                String value = p.value();
                if (!value.equals(""))
                    parameterName = value;
                require = p.require();
            }

            String argClassName = paramClazz.getSimpleName();
            //非数组
            if (!paramClazz.isArray()) {
                String arg = httpUrl.getParameter(parameterName);
                if (arg == null && require) {
                    throw new IllegalArgumentException("parameter [" + parameterName + "] in method " + method.getInvokeMethod().getName() + " shouldn't be null");
                }

                switch (argClassName) {
                    case "String" -> method.setArgument(arg);
                    case "int", "Integer" -> method.setArgument(arg == null ? null : Integer.parseInt(arg));
                    case "long", "Long" -> method.setArgument(arg == null ? null : Long.parseLong(arg));
                    case "double", "Double" -> method.setArgument(arg == null ? null : Double.parseDouble(arg));
                    case "boolean", "Boolean" -> method.setArgument(arg == null ? null : Boolean.parseBoolean(arg));
                    case "Date" -> method.setArgument(arg == null ? null : new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(arg));
                    default -> {
                        Object o = paramClazz.getConstructor().newInstance();
                        generateObj(o, paramClazz, httpUrl, "");
                        method.setArgument(o);
                    }
                }
                continue;
            }

            //数组
            String[] multiArg = httpUrl.getMultiParameter(parameterName);
            if (multiArg == null && require) {
                throw new IllegalArgumentException("parameter [" + parameterName + "] in method " + method.getInvokeMethod().getName() + " shouldn't be null");
            }
            if (multiArg == null) {
                method.setArgument(null);
                continue;
            }
            String className = argClassName.replace("[]", "");
            switch (className) {
                case "String" -> method.setArgument(multiArg);
                case "int" -> {
                    int[] arg = new int[multiArg.length];
                    for (int i = 0; i < multiArg.length; i++) {
                        arg[i] = Integer.parseInt(multiArg[i]);
                    }
                    method.setArgument(arg);
                }
                case "Integer" -> {
                    Integer[] arg = new Integer[multiArg.length];
                    for (int i = 0; i < multiArg.length; i++) {
                        arg[i] = Integer.parseInt(multiArg[i]);
                    }
                    method.setArgument(arg);
                }
                case "long" -> {
                    long[] arg = new long[multiArg.length];
                    for (int i = 0; i < multiArg.length; i++) {
                        arg[i] = Long.parseLong(multiArg[i]);
                    }
                    method.setArgument(arg);
                }
                case "Long" -> {
                    Long[] arg = new Long[multiArg.length];
                    for (int i = 0; i < multiArg.length; i++) {
                        arg[i] = Long.parseLong(multiArg[i]);
                    }
                    method.setArgument(arg);
                }
                default -> throw new UnsupportedOperationException("the parameter in " + method.getInvokeMethod().getName() + " type " + className + "[] is not support!");
            }

        }
    }

    private void generateObj(Object obj, Class<?> objClass, HttpUrl httpUrl, String fatherObjName) {
        try {
            for (Field field : objClass.getDeclaredFields()) {
                String arg = httpUrl.getParameter(fatherObjName + field.getName());
                Class<?> fieldClass = field.getType();
                String fieldClassName = fieldClass.getSimpleName();
                field.setAccessible(true);
                switch (fieldClassName) {
                    case "String" -> field.set(obj, arg);
                    case "int", "Integer" -> field.set(obj, arg == null ? null : Integer.parseInt(arg));
                    case "long", "Long" -> field.set(obj, arg == null ? null : Long.parseLong(arg));
                    case "double", "Double" -> field.set(obj, arg == null ? null : Double.parseDouble(arg));
                    case "boolean", "Boolean" -> field.set(obj, arg == null ? null : Boolean.parseBoolean(arg));
                    case "Date" -> field.set(obj, arg == null ? null : DateUtils.getDate(arg));
                    default -> {
                        Object subObj = fieldClass.getConstructor().newInstance();
                        generateObj(subObj, fieldClass, httpUrl, fatherObjName + field.getName() + ".");
                        field.set(obj, subObj);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }


    private static class HttpUrl {
        @Getter
        private final String url;
        private final Map<String, String[]> parameters;

        HttpUrl(String rowUrl) {
            String rul = RestProcessor.getUrl(rowUrl);
            String[] urlSplit = rul.split("\\?", 2);
            url = urlSplit[0];
            if (urlSplit.length == 2) {
                parameters = new HashMap<>();
                String[] parameterSplit = urlSplit[1].split("&");
                for (String parameterUnit : parameterSplit) {
                    String[] parameterNameAndValue = parameterUnit.split("=", 2);
                    String[] val = null;
                    if (parameterNameAndValue.length == 2) {
                        val = parameterNameAndValue[1].split(",");
                    }
                    parameters.put(parameterNameAndValue[0], val);
                }
            } else parameters = null;
        }

        public String getParameter(String name) {
            if (parameters == null) return null;
            String[] str = parameters.get(name);
            return str == null ? null : str[0];
        }

        public String[] getMultiParameter(String name) {
            if (parameters == null) return null;
            return parameters.get(name);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (cause instanceof ErrorMsgException msgException) {
            responseError(ctx, msgException.getCode(), cause.getMessage());
            return;
        }
        responseError(ctx, cause.getMessage());
    }


    private void responseError(ChannelHandlerContext ctx, String msg) {
        responseError(ctx, 500, msg);
    }

    private void responseError(ChannelHandlerContext ctx, int code, String msg) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code));
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONNECTION, "keep-alive");
        response.headers().set(SERVER, "FNU power by netty");
        response.headers().set(DATE, new Date());
        byte[] bytes = JSON.toJSONBytes(Response.err(code, msg));
        response.headers().set(CONTENT_LENGTH, bytes.length);
        response.content().writeBytes(bytes);
        ctx.writeAndFlush(response);
    }
}
