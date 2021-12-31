package com.freedy.tinyFramework.Expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.tinyFramework.Expression.Comparable;
import com.freedy.tinyFramework.Expression.EvaluationContext;
import com.freedy.tinyFramework.Expression.Executable;
import com.freedy.tinyFramework.exception.EvaluateException;
import com.freedy.tinyFramework.exception.UnsupportedOperationException;
import com.freedy.tinyFramework.utils.ReflectionUtils;
import lombok.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 所有Token的基类,提供一些默认token操作/计算方法。 <br/>
 * 所有继承Token的类需要重写 doCalculate() 或者 doGenericCalculate()方法。 <br/>
 * doCalculate():需要返回该token表示的值。<br/>
 * 例如:<br/>
 * new BasicVarToken('abc');    doCalculate()返回的值就是 "abc"(String)<br/>
 * new BasicVarToken(false);    doCalculate()返回的值就是 false(Boolean)<br/>
 * new ReferenceToken(#Test);   doCalculate()返回的值就是 obj(Object) (命名为Test的对象) <br/>
 * doGenericCalculate(); 该方法与上面方法类似只不过其参数是ParameterizedType 可以根据泛型更加精准的计算结果.
 *
 * @author Freedy
 * @date 2021/12/14 15:33
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@JSONType(includes = {"type", "value"})
public sealed abstract class Token implements Comparable, Executable
        permits BasicVarToken, BlockToken, ClassToken, CollectionToken, ErrMsgToken, IfToken, LoopToken, MapToken, ObjectToken, OpsToken, StopToken, TernaryToken, WrapperToken {
    @ToString.Include
    protected String type;
    @ToString.Include
    protected String value;
    //获取原始token 表示此token是由原始token计算而来
    protected List<Token> originToken;
    //获取子token ,其子token是有该token与其他token计算而来
    protected Token sonToken;
    protected int offset;
    protected List<String> errStr;
    protected Token nextToken;
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
        for (Token t : token) {
            if (t == this) {
                originToken.add(ReflectionUtils.copyProperties(t, "originToken", "sonToken"));
                continue;
            }
            originToken.add(t);
        }
        return this;
    }

    public Token setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public Token errStr(String... str) {
        if (str == null) return this;
        if (errStr == null) {
            errStr = new ArrayList<>();
        }
        errStr.addAll(Arrays.asList(str));
        return this;
    }

    public String getValue() {
        if (notFlag) {
            return "!" + value;
        }
        if (preSelfAddFlag) {
            return "++" + value;
        }
        if (preSelfSubFlag) {
            return "--" + value;
        }
        if (postSelfAddFlag) {
            return value + "++";
        }
        if (postSelfSubFlag) {
            return value + "--";
        }
        return value;
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

    public double compareTo(Token o) {
        BigDecimal a;
        BigDecimal b;
        try {
            a = new BigDecimal(this.calculateResult(Number.class) + "");
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this);
        }
        try {
            b = new BigDecimal(o.calculateResult(Number.class) + "");
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(o);
        }
        return a.subtract(b).doubleValue();
    }

    @SuppressWarnings("ConstantConditions")
    public boolean logicOps(Token o, Token type) {
        boolean a;
        boolean b;
        try {
            a = (boolean) this.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token", e).errToken(this);
        }
        try {
            b = (boolean) o.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token", e).errToken(o);
        }
        switch (type.getValue()) {
            case "||" -> {
                return a || b;
            }
            case "&&" -> {
                return a && b;
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errToken(type);
        }
    }


    public String numSelfOps(Token o, Token type) {
        Object o1;
        Object o2;
        try {
            o1 = calculateResult(Object.class);
            if (o1 == null) {
                throw new EvaluateException("can not operation on null").errToken(this);
            }
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this);
        }
        try {
            o2 = o.calculateResult(Object.class);
            if (o2 == null) {
                throw new EvaluateException("can not operation on null").errToken(o);
            }
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(o);
        }
        if (type.isValue("+")) {
            if (o1 instanceof String || o2 instanceof String) {
                return "string@" + o1 + o2;
            }
        }
        BigDecimal a = new BigDecimal(o1 + "");
        BigDecimal b = new BigDecimal(o2 + "");
        switch (type.getValue()) {
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
                    throw new EvaluateException("nonassignable token").errToken(this).errToken(type);
                }
            }
            case "-=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.subtract(b) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errToken(type);
                }
            }
            case "/=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.divide(b, 20, RoundingMode.DOWN) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errToken(type);
                }
            }
            case "*=" -> {
                if (this instanceof Assignable assignable) {
                    assignable.assignFrom(new BasicVarToken("numeric", a.multiply(b) + ""));
                    return calculateResult(ANY_TYPE) + "";
                } else {
                    throw new EvaluateException("nonassignable token").errToken(this).errToken(type);
                }
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errToken(type);
        }
    }

    private boolean isInteger(Object o) {
        Class<?> wrapper = ReflectionUtils.convertToWrapper(o.getClass());
        return wrapper == Long.class || wrapper == Integer.class;
    }


    protected void checkSetSingleOps(String currentOps, boolean isPost) {
        if (notFlag || preSelfAddFlag || preSelfSubFlag || postSelfAddFlag || postSelfSubFlag)
            throw new EvaluateException("has already set single ops ?", notFlag ? "!" : preSelfAddFlag ? "++a" : preSelfSubFlag ? "--a" : postSelfAddFlag ? "a++" : "a--")
                    .errToken(this.errStr(isPost ? value + "@" + currentOps : currentOps + "$" + value));
    }

    public void setNotFlag(boolean notFlag) {
        checkSetSingleOps("!", false);
        try {
            calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support", e).errToken(this.errStr("!$" + value));
        }
        offset--;
        this.notFlag = notFlag;
    }

    public void setPreSelfAddFlag(boolean preSelfAddFlag) {
        checkSetSingleOps("++", false);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("++a OPS are not support", e).errToken(this.errStr("++" + value));
        }
        offset -= 2;
        this.preSelfAddFlag = preSelfAddFlag;
    }

    public void setPreSelfSubFlag(boolean preSelfSubFlag) {
        checkSetSingleOps("--", false);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("--a OPS are not support", e).errToken(this.errStr("--" + value));
        }
        offset -= 2;
        this.preSelfSubFlag = preSelfSubFlag;
    }

    public void setPostSelfAddFlag(boolean postSelfAddFlag) {
        checkSetSingleOps("++", true);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("a++ OPS are not support", e).errToken(this.errStr(value + "++"));
        }
        this.postSelfAddFlag = postSelfAddFlag;
    }

    public void setPostSelfSubFlag(boolean postSelfSubFlag) {
        checkSetSingleOps("--", true);
        try {
            calculateResult(Integer.class);
        } catch (Exception e) {
            throw new EvaluateException("a-- OPS are not support,cause ?", e).errToken(this.errStr(value + "--"));
        }
        this.postSelfSubFlag = postSelfSubFlag;
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

    protected Object doCalculate(Class<?> desiredType) {
        throw new java.lang.UnsupportedOperationException();
    }

    protected Object doGenericCalculate(ParameterizedType desiredType) {
        throw new java.lang.UnsupportedOperationException();
    }

    protected void checkContext() {
        if (context == null)
            throw new EvaluateException("can not evaluate,because evaluationContext is null");
    }

    protected Object check(Object result) {
        if (result == null) return null;
        if (!ReflectionUtils.convertToWrapper(desiredType).isInstance(result)) {
            Object res = ReflectionUtils.tryConvert(desiredType, result);
            if (res!=Boolean.FALSE){
                return res;
            }
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
        return check(selfOps(result));
    }

    private Object numSelfOps(Object result, int num, boolean isPost) {
        if (result instanceof Number n) {
            Number add = add(n, num);
            if (this instanceof Assignable assignable) {
                assignable.assignFrom(new BasicVarToken("numeric", add + ""));
            } else {
                throw new EvaluateException("SELF OPS are not supported on ? token", this.getType());
            }
            return isPost ? result : add;
        }
        throw new EvaluateException("SELF OPS [?++] on none Numeric type ?", result, result.getClass().getName());
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
