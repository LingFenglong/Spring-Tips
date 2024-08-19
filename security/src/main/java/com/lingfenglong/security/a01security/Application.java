package com.lingfenglong.security.a01security;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@SpringBootTest
class BankAccountServiceTest {
    BankAccountService bankAccountService = new DefaultBankAccountService();

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
    // @WithMockUser(username = "cc", roles = {"ADMIN"})
    public void findByIdManually() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("cc", "idontknow", "ROLE_ADMIN")
        );

        assertDoesNotThrow(
                () -> bankAccountService.findById(1L)
        );
    }

    @Test
    // @WithMockUser(username = "lfl", roles = {"MANAGER"})
    public void findByIdDeniedManually() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("cc", "idontknow", "ROLE_MANAGER")
        );

        assertThrows(
                AuthorizationDeniedException.class,
                () -> bankAccountService.findById(1L)
        );
    }
}

class DefaultBankAccountService implements BankAccountService {

    private static final Logger log = LoggerFactory.getLogger(DefaultBankAccountService.class);

    @Override
    public BankAccount findById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        BankAccount ccBankAccount = new BankAccount(id, "cc", BigDecimal.valueOf(100));

        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new AuthorizationDeniedException(
                    "Only ROLE_ADMIN is supported",
                    () -> false
            );
        } else {
            log.info("{}: {} accessed", getClass().getName(), authentication.getName());
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(authentication, null));
        }

        return ccBankAccount;
    }

    @Override
    public BankAccount getById(Long id) {
        return new BankAccount(id, "lfl", BigDecimal.valueOf(100));
    }
}

interface BankAccountService {

    BankAccount findById(Long id);

    BankAccount getById(Long id);
}

record BankAccount(Long id, String username, BigDecimal account) {
}
