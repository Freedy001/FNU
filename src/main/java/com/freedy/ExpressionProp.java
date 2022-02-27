package com.freedy;

import com.freedy.expression.CommanderLine;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NonStrict;
import com.freedy.tinyFramework.beanFactory.Application;
import lombok.Data;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Freedy
 * @date 2022/2/25 0:03
 */
@Data
@NonStrict
@InjectProperties(value = "expression", nonePutIfEmpty = true)
public class ExpressionProp {

    private boolean enabled = false;
    private int port;
    private String aesKey;
    private String auth;



    @Inject(failFast = true)
    public void init(Application app) {
        if (!enabled) return;
        if (aesKey.length() != 16) throw new IllegalArgumentException("aes-key's length must be 16");
        CommanderLine.setContext(new StanderEvaluationContext() {
            @Override
            public Object getVariable(String name) {
                Object variable = super.getVariable(name);
                if (variable != null) {
                    return variable;
                } else {
                    return app.getBean(filterName(name));
                }
            }

            @Override
            public boolean containsVariable(String name) {
                return super.containsVariable(name) || app.containsBean(name);
            }

            @Override
            public Set<String> allVariables() {
                TreeSet<String> set = new TreeSet<>();
                set.addAll(super.allVariables());
                set.addAll(app.getAllBeanNames());
                return set;
            }
        });
        CommanderLine.startRemote(port,aesKey,auth);
    }

}
