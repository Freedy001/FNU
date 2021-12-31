package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.tokenBuilder.Tokenizer;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 16:57
 */
@Getter
@Setter
@NoArgsConstructor
public abstract sealed class ClassToken extends Token implements Assignable
        permits DirectAccessToken, DotSplitToken, ReferenceToken, StaticToken {
    protected final Pattern strPattern = Pattern.compile("^'(.*?)'$");
    protected final Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");
    protected final Pattern tokenStreamArg = Pattern.compile("^@block\\{(.*)}$");
    private final Pattern relevantOpsPattern = Pattern.compile("(?:\\?|\\[.*]|\\? *?\\[.*])+");
    private final Pattern varPattern = Pattern.compile("^[a-zA-Z_]\\w*");

    protected String reference;
    protected int executableCount = 0;
    protected Expression expression;
    protected String relevantOps;
    private List<ExecuteStep> executeChain;

    @Override
    public void setContext(EvaluationContext context) {
        super.setContext(context);
        expression = new Expression(context);
    }

    public ClassToken(String type, String value) {
        super(type, value);
    }

    public void addProperties(String checkMode, String propertyName) {
        if (executeChain == null) {
            executeChain = new ArrayList<>();
        }
        executeChain.add(new ExecuteStep(checkMode, propertyName));
        executableCount++;
    }

    public void addMethod(String checkMode, String methodName, String... args) {
        if (executeChain == null) {
            executeChain = new ArrayList<>();
        }
        executeChain.add(new ExecuteStep(checkMode, methodName, args));
        executableCount++;
    }

    public void setRelevantOps(String relevantOps) {
        if (relevantOps != null && !relevantOpsPattern.matcher(relevantOps).matches()) {
            throw new IllegalArgumentException("illegal relevantOps ?", relevantOps);
        }
        this.relevantOps = relevantOps;
    }

    public void addAll(List<ExecuteStep> chain) {
        if (executeChain == null) {
            executeChain = new ArrayList<>();
        }
        executeChain.addAll(chain);
    }

    public void clearExecuteChain() {
        executeChain = null;
        executableCount = 0;
        if (isType("static")) {
            value = "T(" + reference + ")";
        }
        if (isType("reference")) {
            value = "#" + reference;
        }
    }

    protected Object executeChain(Class<?> originType, Object origin, int executeSize) {
        return executeChain(originType, origin, executeSize, true);
    }

    /**
     * 对链式编程的表达式进行链式执行
     *
     * @param originType  链条头部对象类型
     * @param origin      链条头部对象
     * @param executeSize 需要执行的数量
     * @return 最后执行的结果
     */
    protected Object executeChain(Class<?> originType, Object origin, int executeSize, boolean executeLastRelevantOps) {
        if (executeChain == null) {
            return origin;
        }
        int size = executeChain.size();
        for (int i = 0; i < size && i < executeSize; i++) {
            ExecuteStep step = executeChain.get(i);
            if (step.isPropMode()) {
                String propertyName = step.getPropertyName();
                try {
                    origin = ReflectionUtils.getter(originType, origin, propertyName);
                } catch (Throwable e) {
                    if (Optional.ofNullable(step.getRelevantOps()).orElse("").trim().equals("?")) {
                        return null;
                    }
                    throw new EvaluateException("get field failed cause:?", e).errToken(this.errStr(propertyName));
                }
            } else {
                Object[] args = getMethodArgs(step.getUnsteadyArgList());
                try {
                    origin = ReflectionUtils.invokeMethod(originType, origin, step.getMethodName(), args);
                } catch (Throwable e) {
                    if (Optional.ofNullable(step.getRelevantOps()).orElse("").trim().equals("?")) {
                        return null;
                    }
                    throw new EvaluateException("invoke target method failed,because ?", e).errToken(this.errStr(step.getStr()));
                }
            }
            try {
                if (i == Math.min(size, executeSize) - 1 && !executeLastRelevantOps) {
                    return origin;
                }
                origin = doRelevantOps(origin, parseOps(step.relevantOps), Math.min(size, executeSize) - i - 1, step.getStr());
            } catch (EvaluateException e) {
                throw e.errToken(this.errStr(e.getErrPart() == null ? new String[]{step.getStr()} : e.getErrPart()));
            }
            if (origin == null) return null;
            originType = origin.getClass();
        }

        return origin;
    }

    /**
     * 讲预处理参数转化为真正的参数
     *
     * @param argsStr 参数字符串数组
     * @return 方法会解析字符串来获取相应的参数
     */
    @NotNull
    protected Object[] getMethodArgs(List<UnsteadyArg> unsteadyArgList) {
        List<Object> args = new ArrayList<>();
        for (UnsteadyArg arg : unsteadyArgList) {
            switch (arg.getType()) {
                case UnsteadyArg.STRING, UnsteadyArg.NUMERIC, UnsteadyArg.DELAY_TOKEN_STREAM -> args.add(arg.getValue());
                case UnsteadyArg.TOKEN_STREAM -> args.add(expression.setTokenStream((TokenStream) arg.getValue()).getValue());
            }
        }
        return args.toArray();
    }

    /**
     * 对方法的参数进行预处理
     */
    protected List<UnsteadyArg> preprocessingArgs(String[] argsStr) {
        List<UnsteadyArg> args = new ArrayList<>();
        for (String methodArg : argsStr) {
            try {
                methodArg = methodArg.trim();
                Matcher matcher = tokenStreamArg.matcher(methodArg);
                if (matcher.find()) {
                    args.add(new UnsteadyArg(UnsteadyArg.DELAY_TOKEN_STREAM, Tokenizer.getTokenStream(matcher.group(1))));
                    continue;
                }
                matcher = strPattern.matcher(methodArg);
                if (matcher.find()) {
                    args.add(new UnsteadyArg(UnsteadyArg.STRING, matcher.group(1)));
                    continue;
                }
                matcher = numeric.matcher(methodArg);
                if (matcher.matches()) {
                    if (new BigDecimal(methodArg).compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
                        if (new BigDecimal(methodArg).compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
                            throw new EvaluateException("? exceed the max of the Long ?", methodArg, Long.MAX_VALUE);
                        }
                        args.add(new UnsteadyArg(UnsteadyArg.NUMERIC, Long.parseLong(methodArg)));
                    } else {
                        args.add(new UnsteadyArg(UnsteadyArg.NUMERIC, Integer.parseInt(methodArg)));
                    }
                    continue;
                }
                args.add(new UnsteadyArg(UnsteadyArg.TOKEN_STREAM, Tokenizer.getTokenStream(methodArg)));
            } catch (ExpressionSyntaxException e) {
                throw e;
            } catch (Throwable e) {
                throw new EvaluateException("get method args ? failed,because ?", methodArg, e).errToken(this.errStr(methodArg));
            }
        }
        return args;
    }

    protected Object doRelevantOps(Object baseObj, String nullMsg) {
        return doRelevantOps(baseObj, parseOps(relevantOps), executableCount, nullMsg);
    }

    protected Object doRelevantOps(Object baseObj, List<String> opsList, int remainExecCount, String nullMsg) {
        return doRelevantOps(baseObj, opsList, remainExecCount, Integer.MAX_VALUE, nullMsg);
    }

    /**
     * 执行类似集合或者map的get操作  <br/>
     * 例如:<br/>
     * List a=List.of(1,2,3,4,5);<br/>
     * ===> a[2]=3;<br/>
     *
     * @param baseObj         基本操作对象
     * @param opsList         相关操作的list,例如 a[2][3] 则list中就是2,3;
     * @param remainExecCount 剩余执行链的长度
     * @param executeCount    需要执行相关操作的数量，例如 a[2][3] executeCount=1 ==> 就只会执行a[2]
     * @param nullMsg         基本对象为空时异常的提示信息
     * @return 计算结果
     */
    protected Object doRelevantOps(Object baseObj, List<String> opsList, int remainExecCount, int executeCount, String nullMsg) {
        if (baseObj == null) {
            if ((opsList != null && opsList.size() >= 1 && opsList.get(0).equals("?")) || remainExecCount == 0) {
                return null;
            }
            throw new EvaluateException("? is a null value", nullMsg);
        }
        if (opsList == null) return baseObj;

        int size = opsList.size();
        for (int i = 0; i < size && i < executeCount; i++) {
            String ops = opsList.get(i);
            if (baseObj == null) {
                if (ops.equals("?")) {
                    return null;
                }
                String builder = getOpsStr(opsList, i);
                throw new EvaluateException("? is a null value", builder).errorPart(builder);
            }
            if (ops.equals("?")) {
                continue;
            }
            Class<?> type = baseObj.getClass();
            expression.setTokenStream(Tokenizer.getTokenStream(ops));
            //执行相关操作
            if (Collection.class.isAssignableFrom(type)) {
                try {
                    if (List.class.isAssignableFrom(type)) {
                        baseObj = ReflectionUtils.invokeMethod(baseObj, "get", expression.getValue(Integer.class));
                    } else {
                        Object[] arr = (Object[]) ReflectionUtils.invokeMethod(baseObj, "toArray");
                        baseObj = arr[expression.getValue(Integer.class)];
                    }
                } catch (Throwable e) {
                    throw new EvaluateException("?", e).errorPart(ops);
                }
            } else if (Map.class.isAssignableFrom(type)) {
                try {
                    baseObj = ReflectionUtils.invokeMethod(baseObj, "get", expression.getValue());
                } catch (Throwable e) {
                    throw new EvaluateException("?", e).errorPart(ops);
                }
            } else {
                throw new EvaluateException("ops[?] can only be used on Collection or Map", ops).errorPart(ops);
            }
        }

        if (baseObj == null && remainExecCount != 0) {
            throw new EvaluateException("? is a null value", nullMsg).errorPart(getOpsStr(opsList, opsList.size()));
        }

        return baseObj;
    }

    /**
     * ClassToken assignFrom方法的工具方法
     *
     * @param relevantOps      相关操作
     * @param baseObjProvider  基类提供者
     * @param resultProvider   需要分配的对象提供者
     * @param normalAssignTask 没有相关操作时调用的分配方法
     */
    @SuppressWarnings("unchecked")
    protected void relevantAssign(String relevantOps, Supplier<Object> baseObjProvider, Supplier<Object> resultProvider, Runnable normalAssignTask) {
        if (StringUtils.isEmpty(relevantOps)) {
            normalAssignTask.run();
        } else {
            List<String> opsList = parseOps(relevantOps);
            int usefulIndex = -1;
            for (int i = opsList.size() - 1; i >= 0; i--) {
                if (!opsList.get(i).equals("?")) {
                    usefulIndex = i;
                    break;
                }
            }
            if (usefulIndex == -1) {
                normalAssignTask.run();
                return;
            }
            Object lastObj = doRelevantOps(baseObjProvider.get(), opsList, 0, usefulIndex, reference);
            if (lastObj == null) {
                throw new EvaluateException("? is a null value", getOpsStr(opsList, usefulIndex));
            }
            String lastOps = opsList.get(usefulIndex);
            expression.setTokenStream(Tokenizer.getTokenStream(lastOps));
            if (lastObj instanceof List list) {
                list.set(expression.getValue(Integer.class), resultProvider.get());
            } else if (lastObj instanceof Map map) {
                map.put(expression.getValue(), resultProvider.get());
            } else {
                throw new EvaluateException("assign ops[?] can only be used on List or Map");
            }
        }
    }


    @NotNull
    protected String getOpsStr(List<String> opsList, int i) {
        String ops;
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            ops = opsList.get(j);
            if (ops.equals("?")) {
                builder.append(ops);
            } else {
                builder.append("[").append(ops).append("]");
            }
        }
        return builder.toString();
    }

    /**
     * 构建相关操作的list
     */
    protected List<String> parseOps(String relevantOps) {
        if (relevantOps == null) return null;
        relevantOps = relevantOps.trim();
        char[] chars = relevantOps.toCharArray();
        int leftBracket = 0;
        List<String> list = new ArrayList<>();
        int lastLeftBracket = -1;
        for (int i = 0; i < chars.length; i++) {

            if (chars[i] == '[') {
                lastLeftBracket = i;
                leftBracket++;
                continue;
            }

            if (chars[i] == ']') {
                leftBracket--;
                if (leftBracket == 0) {
                    String substring = relevantOps.substring(lastLeftBracket + 1, i);
                    if (StringUtils.hasText(substring)) {
                        list.add(substring);
                    }
                }
                continue;
            }

            if (leftBracket > 0) continue;

            if (chars[i] == '?') {
                list.add("?");
            }
        }

        return list;
    }


    protected ExecuteStep getLastPropertyStep() {
        if (executeChain == null) return null;
        ExecuteStep step = executeChain.get(executableCount - 1);
        if (!step.isPropMode()) {
            throw new EvaluateException("the last execute chain element is a method").errToken(this.errStr(step.getStr()));
        }
        return step;
    }

    private ClassToken getThis() {
        return this;
    }

    @Getter
    protected class ExecuteStep {
        private String propertyName;
        private String methodName;
        private String[] methodArgsName;
        private List<UnsteadyArg> unsteadyArgList;
        protected String relevantOps;

        //true->prop false->method
        private final boolean propMode;

        public ExecuteStep(String relevantOps, String propertyName) {
            propMode = true;
            this.propertyName = propertyName;
            this.relevantOps = relevantOps;
        }

        public ExecuteStep(String relevantOps, String methodName, String... methodArgsName) {
            propMode = false;
            this.methodName = methodName;
            this.methodArgsName = methodArgsName;
            this.relevantOps = relevantOps;
            this.unsteadyArgList = getThis().preprocessingArgs(methodArgsName);
        }

        public String getStr() {
            if (propMode) {
                return propertyName + " " + Optional.ofNullable(relevantOps).orElse("");
            }
            return getFullMethodName(methodName, methodArgsName) + " " + Optional.ofNullable(relevantOps).orElse("");
        }
    }

    @Getter
    @AllArgsConstructor
    protected static class UnsteadyArg {
        public static final int STRING = 1 << 1;
        public static final int NUMERIC = 1 << 2;
        public static final int TOKEN_STREAM = 1 << 3;
        public static final int DELAY_TOKEN_STREAM = 1 << 4;

        private int type;
        private Object value;
    }

    public String getFullMethodName(String methodName, String[] methodArgs) {
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (String methodArg : methodArgs) {
            joiner.add(methodArg);
        }
        return methodName + joiner;
    }

}
