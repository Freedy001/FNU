package com.freedy.tinyFramework.beanDefinition;

import com.freedy.tinyFramework.exception.IllegalExpressionException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/7 14:43
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ProxyBeanDefinition extends BeanDefinition {
    private List<MataInterceptor> interceptor = new ArrayList<>();

    public ProxyBeanDefinition(String beanName, Class<?> beanClass) {
        super(beanName, beanClass);
    }

    public void add(MataInterceptor mataInterceptor) {
        interceptor.add(mataInterceptor);
    }


    public List<MataInterceptor> getConditionalIntercept(String beanClassName) {
        //com.freedy.tinyFramework.processor.ProxyProcessor.get*(int,Method,?,*)
        List<MataInterceptor> list = new ArrayList<>();
        for (MataInterceptor mataInterceptor : interceptor) {
            Pattern pattern = Pattern.compile(mataInterceptor.getClassEl());
            if (pattern.matcher(beanClassName).matches()) {
                list.add(mataInterceptor);
            }
        }
        return list;
    }


    @Getter
    public static class MataInterceptor {
        private final Method interceptMethod;
        private final String classEl;
        private final String methodNameEl;
        private final String methodArgEl;
        //注解的全类名
        private final String type;

        private final static Pattern pattern = Pattern.compile("(.*?)\\((.*?)\\)");

        public MataInterceptor(Method interceptMethod, String interceptEL, String type) {
            this.interceptMethod = interceptMethod;
            String[] elSplit = interceptEL.split("\\.");
            int length = elSplit.length;
            if (length < 2) {
                throw new IllegalExpressionException("Illegal Expression Language[?]", interceptEL);
            }
            Matcher matcher = pattern.matcher(elSplit[length - 1]);
            if (!matcher.find())
                throw new IllegalExpressionException("Illegal Expression Language[?],method part should contain like methodName(ParameterType1,ParameterType1)", interceptEL);
            methodNameEl = matcher.group(1);
            methodArgEl = matcher.group(2);
            StringJoiner joiner = new StringJoiner(".");
            for (int i = 0; i < length; i++) {
                if (i == length - 1) break;
                joiner.add(elSplit[i]);
            }
            classEl = joiner.toString();
            this.type = type;
        }


        public String getClassEl() {
            return classEl.replace(".", "\\.").replace("*", ".*?");
        }

        public String getRowClassEl(){
            return classEl;
        }

        public String getMethodArgEl() {
            return methodArgEl.replace(".", "\\.").replace("*", ".*?");
        }

        public String getMethodNameEl() {
            return methodNameEl.replace(".", "\\.").replace("*", ".*?");
        }
    }
}
