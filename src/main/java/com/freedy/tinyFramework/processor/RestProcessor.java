package com.freedy.tinyFramework.processor;

import com.alibaba.fastjson.JSON;
import com.freedy.manage.Response;
import com.freedy.tinyFramework.RequestInterceptor;
import com.freedy.tinyFramework.annotation.mvc.*;
import com.freedy.tinyFramework.beanFactory.BeanFactory;
import com.freedy.tinyFramework.beanFactory.DefaultBeanFactory;
import com.freedy.tinyFramework.exception.ErrorMsgException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final List<RequestInterceptor> interceptorList = new CopyOnWriteArrayList<>();

    public RestProcessor(BeanFactory factory) {
        interceptorList.addAll(factory.getBeansForType(RequestInterceptor.class));
        if (factory instanceof DefaultBeanFactory defaultBeanFactory) {
            defaultBeanFactory.registerBeanAddNotifier((beanFactory, beanName) -> {
                Object bean = beanFactory.getBean(beanName);
                if (bean instanceof RequestInterceptor beanInterceptor) {
                    interceptorList.add(beanInterceptor);
                }
            });
        }
    }

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


        ControllerMethod(String httpMethod, Method method, Object instance) {
            ArgumentInfo[] argumentInfos = new ArgumentInfo[method.getParameterCount()];
            int index = 0;
            //只能出现一个Body注解
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
                    argumentInfos[index++] = new ArgumentInfo(parameter.getName(), param, parameter.getType());
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
        //执行前置拦截器
        invokePreProcessor(req);

        UrlParser urlParser = new UrlParser(req.uri());
        ControllerMethod method = requestHandleMapping.get(urlParser.getUrl());
        if (method == null) {
            log.error("the request url[{}] is not exist", urlParser.url);
            responseError(ctx, 404, "the url your request is not exist");
            return;
        }
        if (!method.getHttpMethodName().contains(req.method().name().toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("The request which url is " + urlParser.getUrl() + " should be " + method.getHttpMethodName() + " method but it is actually " + req.method().name().toLowerCase(Locale.ROOT) + " method");

        fillArguments(req, urlParser, method);

        Object invoke = method.invoke();

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONNECTION, "keep-alive");
        response.headers().set(SERVER, "FNU power by netty");
        response.headers().set(DATE, new Date());
        byte[] bytes = JSON.toJSONBytes(invoke);
        response.headers().set(CONTENT_LENGTH, bytes.length);
        response.content().writeBytes(bytes);

        //执行后置拦截器
        invokePostProcessor(response);

        ctx.writeAndFlush(response);
    }


    private void invokePreProcessor(FullHttpRequest request) {
        //执行后置拦截器
        for (RequestInterceptor interceptor : interceptorList) {
            if (!interceptor.pre(request)) return;
        }
    }

    private void invokePostProcessor(FullHttpResponse response) {
        //执行后置拦截器
        for (RequestInterceptor interceptor : interceptorList) {
            if (!interceptor.post(response)) return;
        }
    }


    private void fillArguments(FullHttpRequest req, UrlParser urlParser, ControllerMethod method) throws Exception {
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

            //是否标有Param注解 方便后面对对象的创建的验证
            boolean hasParam = false;
            //请求参数request param
            boolean require = false;
            String parameterName = argumentInfo.name();
            if (argumentInfo.type instanceof Param p) {
                String value = p.value();
                if (!value.equals(""))
                    parameterName = value;
                require = p.require();
                hasParam = true;
            }

            String httpUrlParameter = urlParser.getParameter(parameterName);
            if (httpUrlParameter == null && require) {
                throw new IllegalArgumentException("parameter [" + parameterName + "] in method " + method.getInvokeMethod().getName() + " shouldn't be null");
            }

            //非数组
            if (!paramClazz.isArray()) {

                if (ReflectionUtils.isBasicType(paramClazz)) {
                    method.setArgument(ReflectionUtils.convertType(httpUrlParameter, paramClazz));
                } else if (ReflectionUtils.isSonInterface(paramClazz, "java.util.Collection", "java.util.Map")) {
                    throw new UnsupportedOperationException("request param doesn't support Collection type or Map type");
                } else {
                    if (!hasParam) continue;
                    Object o = paramClazz.getConstructor().newInstance();
                    generateObj(o, paramClazz, urlParser, "");
                    method.setArgument(o);
                }
                continue;

            }

            //数组
            method.setArgument(ReflectionUtils.buildArrByArrFieldAndVal(paramClazz, httpUrlParameter == null ? null : httpUrlParameter.split(",")));
        }
    }

    private void generateObj(Object obj, Class<?> objClass, UrlParser urlParser, String fatherObjName) {
        try {
            for (Field field : objClass.getDeclaredFields()) {
                String propName = fatherObjName + field.getName();
                Class<?> fieldType = field.getType();
                field.setAccessible(true);

                if (ReflectionUtils.isBasicType(fieldType)) {
                    String arg = urlParser.getParameter(propName);
                    //属性注入
                    field.set(obj, ReflectionUtils.convertType(arg, fieldType));
                } else if (ReflectionUtils.isSonInterface(fieldType, "java.util.Collection")) {
                    String[] arg = urlParser.getMultiParameter(propName);
                    if (arg == null) continue;
                    //属性注入
                    field.set(obj, ReflectionUtils.buildCollectionByFiledAndValue(field, arg));
                } else if (ReflectionUtils.isSonInterface(fieldType, "java.util.Collection")) {
                    //属性注入
                    field.set(obj, ReflectionUtils.buildMapByFiledAndValue(field, propName, urlParser.getOrigin()));
                } else {
                    Object subObj = fieldType.getConstructor().newInstance();
                    generateObj(subObj, fieldType, urlParser, fatherObjName + field.getName() + ".");
                    field.set(obj, subObj);
                }

            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }


    private static class UrlParser {
        @Getter
        private final String url;
        private final Map<String, String> parameters;

        UrlParser(String rowUrl) {
            String rul = RestProcessor.getUrl(rowUrl);
            String[] urlSplit = rul.split("\\?", 2);
            url = urlSplit[0];
            if (urlSplit.length == 2) {
                parameters = new HashMap<>();
                String[] parameterSplit = urlSplit[1].split("&");
                for (String parameterUnit : parameterSplit) {
                    String[] parameterNameAndValue = parameterUnit.split("=", 2);
                    parameters.put(parameterNameAndValue[0], parameterNameAndValue.length == 2 ? parameterNameAndValue[1] : null);
                }
            } else parameters = null;
        }

        public String getParameter(String name) {
            if (parameters == null) return null;
            return parameters.get(name);
        }

        public String[] getMultiParameter(String name) {
            if (parameters == null) return null;
            return parameters.get(name).split(",");
        }

        public Map<String, String> getOrigin() {
            return parameters;
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
        invokePostProcessor(response);
        ctx.writeAndFlush(response);
    }
}
