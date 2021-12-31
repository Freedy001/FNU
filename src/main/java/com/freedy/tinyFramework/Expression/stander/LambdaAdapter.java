package com.freedy.tinyFramework.Expression.stander;

import com.freedy.tinyFramework.exception.IllegalArgumentException;
import lombok.AllArgsConstructor;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import static com.freedy.tinyFramework.Expression.token.Token.ANY_TYPE;

/**
 * @author Freedy
 * @date 2021/12/31 9:26
 */
@AllArgsConstructor
public class LambdaAdapter {

    private StanderFunc.Func func;


    public Object getInstance(Class<?> lambdaType) {
        Method method = getLambdaMethod(lambdaType);
        return Proxy.newProxyInstance(lambdaType.getClassLoader(), new Class[]{lambdaType}, (o, m, a) -> {
            if (method.equals(m))
                return func.apply(a);
            else
                return MethodHandles.lookup()
                        .in(m.getDeclaringClass())
                        .unreflectSpecial(m, m.getDeclaringClass())
                        .bindTo(o)
                        .invokeWithArguments(a);
        });
    }

    private Method getLambdaMethod(Class<?> lambdaType) {
        if (!lambdaType.isInterface()) {
            throw new IllegalArgumentException("lambda only support interface,but ? is not", lambdaType.getName());
        }
        Method lambdaMethod = null;
        for (Method method : lambdaType.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                try {
                    ANY_TYPE.getMethod(method.getName(), method.getParameterTypes());
                } catch (Exception e) {
                    if (lambdaMethod != null) {
                        throw new IllegalArgumentException("? is not lambda interface", lambdaType.getName());
                    }
                    lambdaMethod = method;
                }
            }
        }
        return lambdaMethod;
    }

}
