package com.lingfenglong.proxies.a01jdkproxy;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.*;
import java.lang.reflect.Proxy;
import java.util.Arrays;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return appArgs -> {

            DefaultCustomerService target = new DefaultCustomerService();

            CustomerService proxyInstance = (CustomerService) Proxy.newProxyInstance(
                    target.getClass().getClassLoader(),
                    target.getClass().getInterfaces(),
                    (proxy, method, args) -> {
                        System.out.println("calling " + method.getName() + "with args " + Arrays.toString(args));

                        try {
                            if (method.getAnnotation(MyTransactional.class) != null) {
                                System.out.println("starting transaction for " + method.getName());
                            }

                            return method.invoke(target, args);
                        } finally {
                            if (method.getAnnotation(MyTransactional.class) != null) {
                                // rolling back or clean up
                                System.out.println("finishing transaction for " + method.getName());
                            }
                        }
                    });

            proxyInstance.create();
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
        System.out.println("add DefaultCustomerService create");
    }
}

interface CustomerService {
    @MyTransactional
    void create();  // just a function
}
