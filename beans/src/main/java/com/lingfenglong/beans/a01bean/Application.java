package com.lingfenglong.beans.a01bean;

import com.mysql.cj.jdbc.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.Collection;

public class Application {

}

class ApplicationTest {
    ApplicationContext createApplicationContext() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        ConstructorArgumentValues constructorVals;

        RootBeanDefinition driverBeanDefinition = new RootBeanDefinition(Driver.class);
        beanFactory.registerBeanDefinition("driver", driverBeanDefinition);

        RootBeanDefinition datasourceBeanDefinition = new RootBeanDefinition(SimpleDriverDataSource.class);
        // public SimpleDriverDataSource(java.sql.Driver driver, String url, String username, String password) {}
        constructorVals = datasourceBeanDefinition.getConstructorArgumentValues();
        constructorVals.addGenericArgumentValue(new RuntimeBeanReference("driver"));
        constructorVals.addGenericArgumentValue("jdbc:mysql://127.0.0.1:3306/mydatabase");
        constructorVals.addGenericArgumentValue("root");
        constructorVals.addGenericArgumentValue("root");
        // beanFactory.registerBeanDefinition("datasource", datasourceBeanDefinition);

        RootBeanDefinition jdbcClientBeanDefinition = new RootBeanDefinition(JdbcClient.class);
        jdbcClientBeanDefinition.setFactoryMethodName("create");
        constructorVals = jdbcClientBeanDefinition.getConstructorArgumentValues();
        // constructorVals.addGenericArgumentValue(new RuntimeBeanReference("datasource"));
        constructorVals.addGenericArgumentValue(datasourceBeanDefinition);
        beanFactory.registerBeanDefinition("jdbcClient", jdbcClientBeanDefinition);

        RootBeanDefinition customerServiceBeanDefinition = new RootBeanDefinition(CustomerService.class);
        constructorVals = customerServiceBeanDefinition.getConstructorArgumentValues();
        constructorVals.addGenericArgumentValue(new RuntimeBeanReference("jdbcClient"));
        beanFactory.registerBeanDefinition("customerService", customerServiceBeanDefinition);

        GenericApplicationContext applicationContext = new GenericApplicationContext(beanFactory);
        applicationContext.refresh();
        return applicationContext;
    }

    @Test
    void findAll() {
        ApplicationContext applicationContext = createApplicationContext();

        CustomerService customerService = applicationContext.getBean(CustomerService.class);
        customerService.findAll().forEach(System.out::println);
    }
}

class CustomerService implements InitializingBean {
    private final JdbcClient jdbcClient;

    CustomerService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Collection<Customer> findAll() {
        return jdbcClient.sql("select * from customers")
                .query((rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet...");
    }
}

record Customer(Long id, String name) {

}
