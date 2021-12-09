package com.freedy;

import com.freedy.tinyFramework.annotation.WebApplication;
import com.freedy.tinyFramework.beanFactory.Application;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Freedy
 * @date 2021/11/21 12:20
 */
@Slf4j
@WebApplication(port = 9000)
public class FNUApplication {


    public static void main(String[] args) {
        Application application = new Application(FNUApplication.class).run();
        for (String beanName : application.getAllBeanNames()) {
            System.out.println(beanName+"--->"+application.getBean(beanName));
        }
    }

}
