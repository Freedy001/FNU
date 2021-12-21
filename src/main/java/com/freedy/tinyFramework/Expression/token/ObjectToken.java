package com.freedy.tinyFramework.Expression.token;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/21 22:47
 */
@Getter
@Setter
@NoArgsConstructor
public final class ObjectToken extends Token implements Assignable {
    private Object coreObject;
    private String variableName;

    public ObjectToken(String value) {
        super("obj", value);
    }

    @Override
    public void assignFrom(Token assignment) {
        coreObject = assignment.calculateResult(ANY_TYPE);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        Object check = checkAndSelfOps(coreObject);
        context.setVariable(variableName, check);
        return check;
    }
}
