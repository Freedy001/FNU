package com.freedy.tinyFramework.Expression.token;

import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.UnsupportedOperationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Freedy
 * @date 2021/12/14 15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Token implements Comparable<Token> {
    protected String type;
    protected String value;
    protected EvaluationContext context;
    //非门标记       ! a
    protected boolean notFlag = false;
    //前自加        ++ a
    protected boolean preSelfAddFlag = false;
    //前自减        -- a
    protected boolean preSelfSubFlag = false;
    //后自加        a ++
    protected boolean postSelfAddFlag = false;
    //后自减        a --
    protected boolean postSelfSubFlag = false;

    protected Class<?> desiredType;

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }


    public boolean isAnyType(String... type) {
        for (String s : type) {
            if (isType(s)) return true;
        }
        return false;
    }

    public boolean isType(String type) {
        return type.equals(this.type);
    }

    public boolean isValue(String val) {
        return value.equals(val);
    }


    @SuppressWarnings("ConstantConditions")
    public int compareTo(Token o) {
        long a;
        long b;
        try {
            a = (long) calculateResult(Long.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errToken(o);
        }
        try {
            b = (long) o.calculateResult(Long.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e);
        }
        return (int) (a - b);
    }

    @SuppressWarnings("ConstantConditions")
    public boolean logicOps(Token o, String type) {
        boolean a;
        boolean b;
        try {
            a = (boolean) calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        try {
            b = (boolean) o.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        switch (type) {
            case "||" -> {
                return a || b;
            }
            case "&&" -> {
                return a && b;
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errToken(this).errStr(type).errToken(this);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public double numOps(Token o, String type) {
        double a;
        double b;
        try {
            a = (Double) calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        try {
            b = (Double) o.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        switch (type) {
            case "+" -> {
                return a + b;
            }
            case "-" -> {
                return a - b;
            }
            case "*" -> {
                return a * b;
            }
            case "/" -> {
                return a / b;
            }
            case "+=" -> {
                double v = a + b;
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", v + ""));
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
                return v;
            }
            case "-=" -> {
                double v = a - b;
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", v + ""));
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
                return v;
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errStr(type);
        }
    }


    public final Object calculateResult(Type desiredType) {
        if (desiredType instanceof Class<?> clazz) {
            this.desiredType = clazz;
            return doCalculate(clazz);
        } else if (desiredType instanceof ParameterizedType clazz) {
            this.desiredType = (Class<?>) clazz.getRawType();
            return doGenericCalculate(clazz);
        }
        return null;
    }

    protected void checkSetSingleOps() {
        if (notFlag || preSelfAddFlag || preSelfSubFlag || postSelfAddFlag || postSelfSubFlag)
            throw new EvaluateException("has already set single ops ?", notFlag ? "!" : preSelfAddFlag ? "++a" : preSelfSubFlag ? "--a" : postSelfAddFlag ? "a++" : "a--")
                    .errStr(notFlag ? "!" + value : preSelfAddFlag ? "++" + value : preSelfSubFlag ? "--" + value : postSelfAddFlag ? value + "++" : value + "--");
    }

    public void setNotFlag(boolean notFlag) {
        checkSetSingleOps();
        try {
            calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support").errStr("!" + value);
        }
        this.notFlag = notFlag;
    }

    public void setPreSelfAddFlag(boolean preSelfAddFlag) {
        checkSetSingleOps();
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support").errStr("++" + value);
        }
        this.preSelfAddFlag = preSelfAddFlag;
    }

    public void setPreSelfSubFlag(boolean preSelfSubFlag) {
        checkSetSingleOps();
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support").errStr("--" + value);
        }
        this.preSelfSubFlag = preSelfSubFlag;
    }

    public void setPostSelfAddFlag(boolean postSelfAddFlag) {
        checkSetSingleOps();
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support").errStr(value + "++");
        }
        this.postSelfAddFlag = postSelfAddFlag;
    }

    public void setPostSelfSubFlag(boolean postSelfSubFlag) {
        checkSetSingleOps();
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support").errStr(value + "--");
        }
        this.postSelfSubFlag = postSelfSubFlag;
    }

    protected Object doCalculate(Class<?> desiredType) {
        return null;
    }

    protected Object doGenericCalculate(ParameterizedType desiredType) {
        return null;
    }

    protected void checkContext() {
        if (context == null)
            throw new EvaluateException("can not evaluate,because evaluationContext is null");
    }

    protected Object checkAndSelfOps(Object result) {
        if (!desiredType.isInstance(result)) {
            throw new EvaluateException("unmatched type! real type ? desired type ?", result.getClass().getName(), desiredType.getName());
        }
        if (notFlag) {
            if (result instanceof Boolean r) {
                return !r;
            }
            throw new EvaluateException("NOT OPS on none boolean type ?", result.getClass().getName());
        }
        if (postSelfAddFlag) { //a++
            return numOps(result, 1, true);
        }
        if (postSelfSubFlag) { //a--
            return numOps(result, -1, true);
        }
        if (preSelfAddFlag) { //++a
            return numOps(result, 1, false);
        }
        if (preSelfSubFlag) { //--a
            return numOps(result, -1, false);
        }
        return result;
    }

    private Object numOps(Object result, int num, boolean isPost) {
        if (result instanceof Number n) {
            Number add = add(n, num);
            if (this instanceof Assignable assignable) {
                assignable.assignFrom(new BasicVarToken("numeric", add + ""));
            } else {
                throw new EvaluateException("PRE SELF ADD OPS are not supported on ? token", this.getType());
            }
            return isPost ? result : add;
        }
        throw new EvaluateException("PRE SELF ADD OPS [?++] on none Numeric type ?", result, result.getClass().getName());
    }


    protected Number add(Number n, int b) {
        if (n instanceof Integer a) {
            return a + b;
        }
        if (n instanceof Long a) {
            return a + b;
        }
        if (n instanceof Double a) {
            return a + b;
        }
        if (n instanceof Float a) {
            return a + b;
        }
        if (n instanceof Short a) {
            return a + b;
        }
        throw new UnsupportedOperationException("Number type ? not support", n.getClass().getName());
    }


}
