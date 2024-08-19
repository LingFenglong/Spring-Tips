package com.lingfenglong.security.a06springauto;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootApplication(scanBasePackages = "com.lingfenglong.security.a06springauto")
@EnableMethodSecurity
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@SpringBootTest
class BankAccountServiceTest {
    @Autowired
    BankAccountService bankAccountService;

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
    @WithMockUser(username = "cc", roles = {"ADMIN"})
    public void getAccount() {
        String account = bankAccountService.findById(1L).getAccount();
        assertEquals("123456", account);
    }

    @Test
    @WithMockUser(username = "lfl", roles = {"ADMIN"})
    public void getAccountDeniedWithMask() {
        String account = bankAccountService.findById(1L).getAccount();
        assertEquals("******", account);
    }
}

@Service
class DefaultBankAccountService implements BankAccountService {
    @PreReadBankAccount
    @Override
    public BankAccount findById(Long id) {
        return new BankAccount(id, "cc", "123456", BigDecimal.valueOf(100));
    }

    @PreReadBankAccount
    @Override
    public BankAccount getById(Long id) {
        return findById(id);
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PostAuthorize("returnObject.getUsername() == authentication?.name or hasAuthority('ROLE_ADMIN')")
@AuthorizeReturnObject  // used to proxy return value
@interface PreReadBankAccount {

}

interface BankAccountService {
    BankAccount findById(Long id);

    BankAccount getById(Long id);
}

class BankAccount {
    private final Long id;
    private final String username;
    private final String account;
    private final BigDecimal balance;

    BankAccount(Long id, String username, String account, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.account = account;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @PreAuthorize("this.username == authentication?.name")
    @HandleAuthorizationDenied(handlerClass = MaskMethodAuthorizationDeniedHandler.class)   // record not support this
    public String getAccount() {
        return account;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username) && Objects.equals(account, that.account) && Objects.equals(balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, account, balance);
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", account='" + account + '\'' +
                ", balance=" + balance +
                '}';
    }
}

@Component
class MaskMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {

    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        return "******";
    }
}