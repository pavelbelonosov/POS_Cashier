package com.app.bank_acquiring;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.service.AccountService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountServiceTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    @Before
    public void setUp() {

    }

    @Test
    public void whenCreateAdmin_thenSetsAdminAuthority() {
        Account admin = new Account();
        admin.setUsername("username");
        admin.setPassword("password");
        accountService.createAdminUser(admin, new AccountInfo());
        Account acc = accountRepository.findByUsername(admin.getUsername());
        assertTrue(acc.getAuthority() == Authority.ADMIN);
    }


}
