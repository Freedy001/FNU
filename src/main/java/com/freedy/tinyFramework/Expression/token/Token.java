package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.UnsupportedOperationException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/14 15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JSONType(includes = {"type", "value"})
public abstract class Token implements Comparable<Token> {
    protected String type;
    protected String value;
    //获取原始token 表示此token是由原始token计算而来
    protected List<Token> originToken;
    //获取子token ,其子token是有该token与其他token计算而来
    protected Token sonToken;
    protected int offset;
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

    public final static Class<Object> ANY_TYPE = Object.class;

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean isType(String type) {
        return type.equals(this.type);
    }

    public boolean isValue(String val) {
        return value.equals(val);
    }

    public boolean isAnyType(String... type) {
        for (String s : type) {
            if (isType(s)) return true;
        }
        return false;
    }

    public boolean isAnyValue(String... type) {
        for (String s : type) {
            if (isValue(s)) return true;
        }
        return false;
    }

    public Token setOriginToken(Token... token) {
        if (originToken == null) {
            originToken = new ArrayList<>();
        }
        originToken.addAll(Arrays.asList(token));
        return this;
    }


    public Token setOffset(int offset){
        this.offset=offset;
        return this;
    }

    public List<Token> getMostOriginToken() {
        if (originToken == null) return new ArrayList<>(List.of(this));
        Queue<Token> queue = new LinkedList<>(originToken);
        ArrayList<Token> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Token poll = queue.poll();
            if (poll.originToken != null) {
                queue.addAll(poll.originToken);
            } else {
                result.add(poll);
            }
        }
        return result;
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
            throw new EvaluateException("incomparable token", e).errToken(this).errStr(type).errToken(o);
        }
        try {
            b = (boolean) o.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token", e).errToken(this).errStr(type).errToken(o);
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


    public String numSelfOps(Token o, String type) {
        BigDecimal a;
        BigDecimal b;
        try {
            a = new BigDecimal(calculateResult(Number.class) + "");
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        try {
            b = new BigDecimal(o.calculateResult(Number.class) + "");
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this).errStr(type).errToken(this);
        }
        switch (type) {
            case "+" -> {
                return a.add(b).toString();
            }
            case "-" -> {
                return a.subtract(b).toString();
            }
            case "*" -> {
                return a.multiply(b).toString();
            }
            case "/" -> {
                //scale 必须超出double的范畴
                return a.divide(b, 20, RoundingMode.DOWN).toString();
            }
            case "+=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.add(b) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
            }
            case "-=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.subtract(b) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
            }
            case "/=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.divide(b, 20, RoundingMode.DOWN) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
            }
            case "*=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.multiply(b) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errStr(type).errToken(this);
                }
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

    protected void checkSetSingleOps(String currentOps, boolean isPost) {
        if (notFlag || preSelfAddFlag || preSelfSubFlag || postSelfAddFlag || postSelfSubFlag)
            throw new EvaluateException("has already set single ops ?", notFlag ? "!" : preSelfAddFlag ? "++a" : preSelfSubFlag ? "--a" : postSelfAddFlag ? "a++" : "a--")
                    .errStr(isPost ? value + "@" + currentOps : currentOps + "$" + value);
    }

    public void setNotFlag(boolean notFlag) {
        checkSetSingleOps("!", false);
        try {
            calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support", e).errStr("!$" + value);
        }
        this.notFlag = notFlag;
    }

    public void setPreSelfAddFlag(boolean preSelfAddFlag) {
        checkSetSingleOps("++", false);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("++a OPS are not support", e).errStr("++" + value);
        }
        this.preSelfAddFlag = preSelfAddFlag;
    }

    public void setPreSelfSubFlag(boolean preSelfSubFlag) {
        checkSetSingleOps("--", false);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("--a OPS are not support", e).errStr("--" + value);
        }
        this.preSelfSubFlag = preSelfSubFlag;
    }

    public void setPostSelfAddFlag(boolean postSelfAddFlag) {
        checkSetSingleOps("++", true);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("a++ OPS are not support", e).errStr(value + "++");
        }
        this.postSelfAddFlag = postSelfAddFlag;
    }

    public void setPostSelfSubFlag(boolean postSelfSubFlag) {
        checkSetSingleOps("--", true);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("a-- OPS are not support,cause ?", e).errStr(value + "--");
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

    protected Object check(Object result) {
        if (!ReflectionUtils.convertToWrapper(desiredType).isInstance(result)) {
            throw new EvaluateException("unmatched type! real type ? desired type ?", result.getClass().getName(), desiredType.getName());
        }
        return result;
    }

    protected Object selfOps(Object result) {
        if (notFlag) {
            if (result instanceof Boolean r) {
                return !r;
            }
            throw new EvaluateException("NOT OPS on none boolean type ?", result.getClass().getName());
        }
        if (postSelfAddFlag) { //a++
            return numSelfOps(result, 1, true);
        }
        if (postSelfSubFlag) { //a--
            return numSelfOps(result, -1, true);
        }
        if (preSelfAddFlag) { //++a
            return numSelfOps(result, 1, false);
        }
        if (preSelfSubFlag) { //--a
            return numSelfOps(result, -1, false);
        }
        return result;
    }

    protected Object checkAndSelfOps(Object result) {
        return selfOps(check(result));
    }

    private Object numSelfOps(Object result, int num, boolean isPost) {
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
