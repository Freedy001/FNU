package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.TokenStream;
import com.freedy.tinyFramework.Expression.Tokenizer;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 16:57
 */
@Getter
@Setter
@NoArgsConstructor
public abstract sealed class ClassToken extends Token implements Assignable permits DotSplitToken, ReferenceToken, StaticToken {
    private final Pattern strPattern = Pattern.compile("^'(.*?)'$");
    private final Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");

    protected boolean checkMode;
    protected String reference;
    protected int executableCount = 0;
    private List<ExecuteStep> executeChain;

    public ClassToken(String type, String value) {
        super(type, value);
    }

    public void addProperties(boolean checkMode, String propertyName) {
        if (executeChain == null) {
            executeChain = new ArrayList<>();
        }
        executeChain.add(new ExecuteStep(checkMode, propertyName));
        executableCount++;
    }

    public void addMethod(boolean checkMode, String methodName, String... args) {
        if (executeChain == null) {
            executeChain = new ArrayList<>();
        }
        executeChain.add(new ExecuteStep(checkMode, methodName, args));
        executableCount++;
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
                } catch (Exception e) {
                    throw new EvaluateException("get field failed cause:?", e).errToken(this.errStr(propertyName));
                }
            } else {
                List<Object> args = new ArrayList<>();
                for (String methodArg : step.getMethodArgs()) {
                    try {
                        Matcher matcher = strPattern.matcher(methodArg);
                        if (matcher.find()) {
                            args.add(matcher.group(1));
                            continue;
                        }
                        matcher = numeric.matcher(methodArg);
                        if (matcher.matches()) {
                            if (new BigDecimal(methodArg).compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
                                if (new BigDecimal(methodArg).compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
                                    throw new EvaluateException("? exceed the max of the Long ?", methodArg, Long.MAX_VALUE);
                                }
                                args.add(Long.parseLong(methodArg));
                            } else {
                                args.add(Integer.parseInt(methodArg));
                            }
                            continue;
                        }
                        TokenStream stream = Tokenizer.getTokenStream(methodArg);
                        args.add(new Expression(stream).getValue(context));
                    } catch (ExpressionSyntaxException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new EvaluateException("get method args ? failed,because ?", methodArg, e).errToken(this.errStr(methodArg));
                    }
                }
                try {
                    origin = ReflectionUtils.invokeMethod(originType, origin, step.getMethodName(), args.toArray());
                } catch (Exception e) {
                    throw new EvaluateException("invoke target method failed,because ?", e).errToken(this.errStr(step.getStr()));
                }
            }
            if (i == size - 1) return origin;
            if (origin == null) {
                if (step.isCheckMode()) {
                    return null;
                }
                StringJoiner joiner = new StringJoiner(".");
                for (int j = 0; j < i; j++) {
                    joiner.add(executeChain.get(j).getStr());
                }
                throw new EvaluateException("Null value returned during execution").errToken(this.errStr(StringUtils.hasText(joiner.toString()) ? (joiner + ".@" + step.getStr()) : step.getStr()));
            }
            originType = origin.getClass();
        }

        return origin;
    }


    protected String getLastPropertyName() {
        if (executeChain == null) return null;
        ExecuteStep step = executeChain.get(executableCount - 1);
        if (!step.isPropMode()) {
            throw new EvaluateException("the last execute chain element is a method").errToken(this.errStr(step.getStr()));
        }
        return step.getPropertyName();
    }

    @Getter
    public static class ExecuteStep {
        private String propertyName;
        private String methodName;
        private String[] methodArgs;
        protected boolean checkMode;

        //true->prop false->method
        private final boolean propMode;

        public ExecuteStep(boolean checkMode, String propertyName) {
            propMode = true;
            this.propertyName = propertyName;
            this.checkMode = checkMode;
        }

        public ExecuteStep(boolean checkMode, String methodName, String... methodArgs) {
            propMode = false;
            this.methodName = methodName;
            this.methodArgs = methodArgs;
            this.checkMode = checkMode;
        }

        public String getStr() {
            if (propMode) {
                return propertyName + (checkMode ? "?" : "");
            }
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            for (String methodArg : methodArgs) {
                joiner.add(methodArg);
            }
            return methodName + joiner;
        }
    }

}
