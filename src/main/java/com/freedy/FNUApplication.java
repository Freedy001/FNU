package com.freedy;

import com.freedy.expression.CommanderLine;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.tinyFramework.annotation.WebApplication;
import com.freedy.tinyFramework.beanFactory.Application;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.TreeSet;


/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
@Slf4j
@WebApplication(port = 9000)
public class FNUApplication {


    public static void main(String[] args) {
        Application app = new Application(FNUApplication.class).run();
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
        CommanderLine.startRemote(Integer.parseInt(args[0]), "abcdefrtgszxcsqw", "asfasfasfasfasx");
    }

}
