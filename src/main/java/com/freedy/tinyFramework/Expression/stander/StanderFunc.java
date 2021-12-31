package com.freedy.tinyFramework.Expression.stander;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.StanderEvaluationContext;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.function.Consumer;
import com.freedy.tinyFramework.Expression.function.Function;
import com.freedy.tinyFramework.Expression.function.VarConsumer;
import com.freedy.tinyFramework.Expression.function.VarFunction;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.PlaceholderParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

import static com.freedy.tinyFramework.utils.ReflectionUtils.convertToWrapper;
import static com.freedy.tinyFramework.utils.ReflectionUtils.tryConvert;

/**
 * 表达式默认方法定义
 *
 * @author Freedy
 * @date 2021/12/24 21:45
 */
@AllArgsConstructor
public class StanderFunc {

    private final EvaluationContext context;


    public void registerStanderFunc() {
        context.registerFunction("print", (Consumer._1ParameterConsumer<Object>) System.out::println);

        context.registerFunction("printInline", (Consumer._1ParameterConsumer<Object>) System.out::print);

        context.registerFunction("range", (Function._2ParameterFunction<Integer, Integer, List<Integer>>) (a, b) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i++) {
                list.add(i);
            }
            return list;
        });

        context.registerFunction("stepRange", (Function._3ParameterFunction<Integer, Integer, Integer, List<Integer>>) (a, b, step) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i += step) {
                list.add(i);
            }
            return list;
        });

        context.registerFunction("new", (VarFunction._2ParameterFunction<String, Object, Object>) (className, args) -> {
            Class<?> aClass = Class.forName(className);
            List<Constructor<?>> constructorList = new ArrayList<>();
            List<Constructor<?>> seminary = new ArrayList<>();
            int length = args.length;
            for (Constructor<?> cst : aClass.getConstructors()) {
                if (cst.getParameterCount() == length) {
                    constructorList.add(cst);
                }
                seminary.add(cst);
            }
            for (Constructor<?> constructor : constructorList) {
                Class<?>[] types = constructor.getParameterTypes();
                int i = 0;
                for (; i < length; i++) {
                    Class<?> originMethodArgs = convertToWrapper(types[i]);
                    Class<?> supplyMethodArgs = convertToWrapper(args[i] == null ? types[i] : args[i].getClass());
                    if (!originMethodArgs.isAssignableFrom(supplyMethodArgs)) {
                        Object o = tryConvert(originMethodArgs, args[i]);
                        if (o != Boolean.FALSE) {
                            args[i] = o;
                        } else {
                            break;
                        }
                    }
                }
                if (i == length) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
            StringJoiner argStr = new StringJoiner(",", "(", ")");
            for (Object arg : args) {
                argStr.add(arg.getClass().getName());
            }
            throw new NoSuchMethodException("no constructor" + argStr + "!you can call these constructors:" + new PlaceholderParser("?*", seminary.stream().map(method -> {
                StringJoiner argString = new StringJoiner(",", "(", ")");
                for (Parameter arg : method.getParameters()) {
                    argString.add(arg.getType().getSimpleName() + " " + arg.getName());
                }
                return method.getName() + argString;
            }).toList()).serialParamsSplit(" , ").ifEmptyFillWith("not find matched method"));
        });

        context.registerFunction("newInterface", (VarFunction._2ParameterFunction<String, Object, Object>) (name, funcPara) -> {
            Class<?> clazz = Class.forName(name);
            if (!clazz.isInterface()) {
                throw new IllegalArgumentException("? is not interface", name);
            }
            int len = funcPara.length;
            List<Object[]> rowFuncList = new ArrayList<>();
            int lastSplit = 0;
            for (int i = 0; i < len; i++) {
                if (funcPara[i] instanceof TokenStream) {
                    rowFuncList.add(Arrays.copyOfRange(funcPara, lastSplit, lastSplit = i + 1));
                }
            }
            Method[] declaredMethods = clazz.getDeclaredMethods();
            int methodCount = declaredMethods.length;
            int rawMethodCount = rowFuncList.size();
            if (methodCount != rawMethodCount) {
                throw new IllegalArgumentException("the method count[?] which you declare is not match the interface[?]'s method count[?]", rawMethodCount, name, methodCount);
            }
            Map<String, Func> nameFuncMapping = rowFuncList.stream().map(this::getFunc).collect(Collectors.toMap(Func::getFuncName, o -> o));
            for (Method method : declaredMethods) {
                Func func = nameFuncMapping.get(method.getName());
                if (func == null) {
                    throw new IllegalArgumentException("you don't declare the method ?", method.getName());
                } else {
                    if (method.getParameterCount() != func.getArgName().length) {
                        throw new IllegalArgumentException("your method[?] parameter count[?] is not match to the interface's[?]", method.getName(), func.getArgName().length, method.getParameterCount());
                    }
                }
            }
            return Proxy.newProxyInstance(StanderFunc.class.getClassLoader(), new Class[]{clazz}, (proxy, method, args) -> nameFuncMapping.get(method.getName()).apply(args));
        });

        context.registerFunction("lambda", (VarFunction._1ParameterFunction<Object, LambdaAdapter>) par -> {
            Object[] newParam = new Object[par.length + 1];
            System.arraycopy(par, 0, newParam, 1, par.length);
            newParam[0] = "lambda";
            return new LambdaAdapter(getFunc(newParam));
        });

        context.registerFunction("func", (VarConsumer._1ParameterConsumer<Object>) funcPar -> {
            Func func = getFunc(funcPar);
            if (context.containsFunction(func.getFuncName())) {
                throw new EvaluateException("same method name ?!", func.getFuncName());
            }
            context.registerFunction(func.getFuncName(), func);
        });

        context.registerFunction("class", (Function._1ParameterFunction<Object, Class<?>>) arg -> {
            if (arg instanceof String s) {
                return Class.forName(s);
            } else {
                if (arg == null) return null;
                return arg.getClass();
            }
        });
    }

    @NotNull
    private Func getFunc(Object[] funcPar) {
        int length = funcPar.length;
        if (length <= 1) throw new IllegalArgumentException("func() must specify function body");
        if (!(funcPar[0] instanceof String)) throw new IllegalArgumentException("func()'s fist arg must be string");
        Func func = new Func();
        func.setFuncName((String) funcPar[0]);
        func.setArgName(Arrays.stream(Arrays.copyOfRange(funcPar, 1, length - 1)).map(arg -> {
            if (arg instanceof String str) {
                return str;
            } else {
                throw new IllegalArgumentException("func()'s fist arg must be string");
            }
        }).toArray(String[]::new));
        func.setFuncBody((TokenStream) funcPar[length - 1]);
        return func;
    }

    @Setter
    @Getter
    public class Func implements VarFunction._1ParameterFunction<Object, Object> {
        private String funcName;
        private String[] argName;
        private TokenStream funcBody;
        private final StanderEvaluationContext subContext = new StanderEvaluationContext(context.getRoot());
        private final EvaluationContext proxy = (EvaluationContext) Proxy.newProxyInstance(subContext.getClass().getClassLoader(), subContext.getClass().getInterfaces(), (proxy1, method, args) -> {
            String methodName = method.getName();
            if (methodName.equals("getVariable")) {
                String varName = (String) args[0];
                //@ 开头直接去父容器拿值
                if (varName.startsWith("@")) {
                    if (context.containsVariable(varName)) {
                        return context.getVariable(varName);
                    }
                    throw new EvaluateException("no var ? in the context", varName);
                }
                //# 开头先去子容器拿值如果没有就去父容器拿值
                if (subContext.containsVariable(varName)) {
                    return subContext.getVariable(varName);
                }
                if (context.containsVariable(varName)) {
                    return context.getVariable(varName);
                }
                throw new EvaluateException("no var ? in the context", varName);
            }
            if (methodName.equals("setVariable")) {
                String varName = (String) args[0];
                StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
                if (stackTraceElement.getClassName().equals("com.freedy.tinyFramework.Expression.token.ObjectToken")) {
                    if (!subContext.containsVariable(varName)) {
                        return subContext.setVariable(varName, args[1]);
                    }
                    throw new EvaluateException("you have already def ?", varName);
                } else if (stackTraceElement.getClassName().equals("com.freedy.tinyFramework.Expression.token.LoopToken")) {
                    return subContext.setVariable(varName, args[1]);
                } else {
                    if (varName.startsWith("@")) {
                        return context.setVariable(varName, args[1]);
                    }
                    if (subContext.containsVariable(varName)) {
                        return subContext.setVariable(varName, args[1]);
                    }
                    if (context.containsVariable(varName)) {
                        return context.setVariable(varName, args[1]);
                    }
                    return subContext.setVariable(varName, args[1]);
                }
            }
            if (methodName.matches("containsVariable")) {
                String varName = (String) args[0];
                if (Thread.currentThread().getStackTrace()[3].getClassName().equals("com.freedy.tinyFramework.Expression.token.LoopToken")) {
                    return subContext.containsVariable(varName);
                }
                if (!varName.startsWith("@")) {
                    if (subContext.containsVariable(varName)) {
                        return true;
                    }
                }
                return context.containsVariable(varName);
            }
            if (methodName.matches("containsFunction|getFunction|registerFunction")) {
                return method.invoke(context, args);
            }
            return method.invoke(subContext, args);
        });



        @Override
        public Object apply(Object... obj) throws Exception {
            if (obj != null && obj.length != argName.length) {
                throw new IllegalArgumentException("unmatched args ?(?*)", funcBody, argName);
            }
            subContext.clearVariable();
            if (obj != null) {
                for (int i = 0; i < argName.length; i++) {
                    subContext.setVariable(argName[i], obj[i]);
                }
            }
            Expression expression = new Expression(funcBody, proxy);
            return expression.getValue();
        }
    }

}
