package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DefaultControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @After
    public void tearDown() {
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
    }


    @Test
    public void whenDoDefaultRedirect_thenRedirectToMainPage() throws Exception {
        mockMvc.perform(get("/").with(anonymous()))
                .andExpect(redirectedUrl("/main"));

        Account admin = createUserInRepository(Authority.ADMIN);
        mockMvc.perform(get("/")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(redirectedUrl("/main"));

    }

    @Test
    public void givenAdminUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        mockMvc.perform(formLogin("/login.html").user(admin.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(admin.getUsername()))
                .andExpect(authenticated().withAuthorities(getAuthorities(admin)));
    }

    @Test
    public void givenCashierUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account cashier = createUserInRepository(Authority.CASHIER);
        mockMvc.perform(formLogin("/login.html").user(cashier.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(cashier.getUsername()))
                .andExpect(authenticated().withAuthorities(getAuthorities(cashier)));
    }

    @Test
    public void givenHeadCashierUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account headCashier = createUserInRepository(Authority.HEAD_CASHIER);
        mockMvc.perform(formLogin("/login.html").user(headCashier.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(headCashier.getUsername()))
                .andExpect(authenticated().withAuthorities(getAuthorities(headCashier)));
    }

    @Test
    public void givenWrongLoginPassword_whenLogin_thenRedirectToLoginErrorPage() throws Exception {
        mockMvc.perform(formLogin("/login.html").user("wrongUsername").password("password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login-error.html"));
    }

    private Account createUserInRepository(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(passwordEncoder.encode("password"));
        user.setAuthority(authority);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccount(user);
        accountInfoRepository.save(accountInfo);
        return accountRepository.save(user);
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }
}
