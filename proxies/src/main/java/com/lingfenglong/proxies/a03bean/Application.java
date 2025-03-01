package com.lingfenglong.proxies.a03bean;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    static boolean transactional(Object o) {
        AtomicBoolean hasTransaction = new AtomicBoolean(false);
        ArrayList<Class<?>> classes = new ArrayList<>();
        classes.add(o.getClass());
        Collections.addAll(classes, o.getClass().getInterfaces());

        classes.forEach(clazz -> ReflectionUtils.doWithMethods(clazz, method -> {
            if (method.getAnnotation(MyTransactional.class) != null) {
                hasTransaction.set(true);
            }
        }));
        return hasTransaction.get();
    }

    @Bean
    MyTransactionBeanPostProcessor myTransactionBeanPostProcessor() {
        return new MyTransactionBeanPostProcessor();
    }

    static class MyTransactionBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object target, String beanName) throws BeansException {
            if (transactional(target)) {
                ProxyFactory pf = new ProxyFactory(target);
                pf.setTarget(target);
                pf.setInterfaces(target.getClass().getInterfaces());
                pf.addAdvice((MethodInterceptor) methodInvocation -> {
                    Method method = methodInvocation.getMethod();
                    Object[] arguments = methodInvocation.getArguments();

                    System.out.println("calling " + method.getName() + "with args " + Arrays.toString(arguments));

                    try {
                        if (method.getAnnotation(MyTransactional.class) != null) {
                            System.out.println("starting transaction for " + method.getName());
                        }

                        return method.invoke(target, arguments);
                    } finally {
                        if (method.getAnnotation(MyTransactional.class) != null) {
                            // rolling back or clean up
                            System.out.println("finishing transaction for " + method.getName());
                        }
                    }
                });

                return pf.getProxy();
            }
            return BeanPostProcessor.super.postProcessAfterInitialization(target, beanName);
        }
    }

    @Bean
    CustomerService customerService() {
        return new DefaultCustomerService();
    }

    @Bean
    ApplicationRunner applicationRunner(CustomerService customerService) {
        return appArgs -> {
            customerService.create();

            customerService.add();
        };
    }
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
@interface MyTransactional {

}

class DefaultCustomerService implements CustomerService {

    @Override
    public void create() {
        System.out.println("DefaultCustomerService create");
    }

    @Override
    public void add() {
        System.out.println("DefaultCustomerService add");
    }
}

interface CustomerService {
    @MyTransactional
    void create();  // just a function

    void add();
}
