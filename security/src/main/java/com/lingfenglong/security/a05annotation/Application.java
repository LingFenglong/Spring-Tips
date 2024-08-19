package com.lingfenglong.security.a05annotation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.method.AuthorizationAdvisorProxyFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootApplication
@EnableMethodSecurity
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@SpringBootTest
class BankAccountServiceTest {
    AuthorizationAdvisorProxyFactory proxyFactory = AuthorizationAdvisorProxyFactory.withDefaults();
    BankAccountService bankAccountService = (BankAccountService) proxyFactory.proxy(new DefaultBankAccountService());

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

    @Test
    @WithMockUser(username = "cc")
    public void findByIdWhenUsername() {
        assertDoesNotThrow(
                () -> bankAccountService.findById(1L)
        );
    }

    @Test
    @WithMockUser(username = "lfl")
    public void findByIdWhenUsernameDenied() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> bankAccountService.findById(1L)
        );
    }

    @Test
    @WithMockUser(username = "lfl", roles = {"ADMIN"})
    public void getAccountDenied() {
        assertThrows(
                AuthorizationDeniedException.class,
                () -> bankAccountService.findById(1L).account()
        );
    }
}

class DefaultBankAccountService implements BankAccountService {
    @PreReadBankAccount
    @Override
    public Account findById(Long id) {
        AuthorizationAdvisorProxyFactory proxyFactory = AuthorizationAdvisorProxyFactory.withDefaults();
        BankAccount bankAccount = new BankAccount(id, "cc", "123456", BigDecimal.valueOf(100));

        // proxy return value
        return (Account) proxyFactory.proxy(bankAccount);
    }


    /**
     * Proxied method self-invocation (in effect, a method within the target object calling another method of
     * the target object). The annotation will be ignored at runtime.
     */
    @PreReadBankAccount
    @Override
    public Account getById(Long id) {
        return findById(id);
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PostAuthorize("returnObject.username() == authentication?.name or hasAuthority('ROLE_ADMIN')")
@interface PreReadBankAccount {

}

interface BankAccountService {
    // @PreReadBankAccount
    Account findById(Long id);

    // @PreReadBankAccount
    Account getById(Long id);
}

record BankAccount(Long id, String username, String account, BigDecimal balance) implements Account {
    @PreAuthorize("this.username == authentication?.name")
    @Override
    public String account() {
        return account;
    }
}

interface Account {
    Long id();
    String username();
    String account();
    BigDecimal balance();
}
