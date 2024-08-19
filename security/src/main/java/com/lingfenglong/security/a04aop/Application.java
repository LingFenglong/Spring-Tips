package com.lingfenglong.security.a04aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@SpringBootTest
class BankAccountServiceTest {
    private static final Logger log = LoggerFactory.getLogger(BankAccountServiceTest.class);

    AopProxy aopProxy;
    BankAccountService bankAccountService;

    {
        DefaultBankAccountService target = new DefaultBankAccountService();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            if (invocation.getMethod().getName().equals("findById")) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                    throw new AuthorizationDeniedException(
                            "Only ROLE_ADMIN is supported",
                            () -> false
                    );
                }

                log.info("{}: {} accessed", getClass().getName(), authentication.getName());
                SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(authentication, null));
            }

            return invocation.proceed();
        });

        aopProxy = DefaultAopProxyFactory.INSTANCE
                .createAopProxy(proxyFactory);

        bankAccountService = (BankAccountService) aopProxy.getProxy();
    }

    @Test
    @WithMockUser(username = "cc", roles = {"ADMIN"})
    public void findById() {
        assertDoesNotThrow(
                () -> bankAccountService.findById(1L)
        );
    }

    @Test
    @WithMockUser(username = "lfl", roles = {"MANAGER"})
    public void findByIdDenied() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> bankAccountService.findById(1L)
        );
    }

    @Test
    @WithMockUser(username = "cc", roles = {"ADMIN"})
    public void getById() {
        assertDoesNotThrow(
                () -> bankAccountService.getById(1L)
        );
    }

    @Test
    @WithMockUser(username = "lfl", roles = {"MANAGER"})
    public void getByIdDenied() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> bankAccountService.getById(1L)
        );
    }
}

class DefaultBankAccountService implements BankAccountService {

    @Override
    public BankAccount findById(Long id) {
        return new BankAccount(id, "cc", BigDecimal.valueOf(100));
    }

    @Override
    public BankAccount getById(Long id) {
        return findById(id);
    }
}

interface BankAccountService {

    BankAccount findById(Long id);

    BankAccount getById(Long id);
}

record BankAccount(Long id, String username, BigDecimal account) {
}
