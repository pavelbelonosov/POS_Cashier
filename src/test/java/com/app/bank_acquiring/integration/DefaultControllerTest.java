package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
    private UtilPopulate utilPopulate;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @After
    public void tearDown() {
        utilPopulate.clearTables();
    }


    @Test
    public void whenDoDefaultRedirect_thenRedirectToMainPage() throws Exception {
        mockMvc.perform(get("/").with(anonymous()))
                .andExpect(redirectedUrl("/main"));

        Account admin = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(get("/")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(redirectedUrl("/main"));

    }

    @Test
    public void givenAdminUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(formLogin("/login.html").user(admin.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(admin.getUsername()))
                .andExpect(authenticated().withAuthorities(utilPopulate.getAuthorities(admin)));
    }

    @Test
    public void givenCashierUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        mockMvc.perform(formLogin("/login.html").user(cashier.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(cashier.getUsername()))
                .andExpect(authenticated().withAuthorities(utilPopulate.getAuthorities(cashier)));
    }

    @Test
    public void givenHeadCashierUser_whenLogin_thenAuthorizeUser() throws Exception {
        Account headCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        mockMvc.perform(formLogin("/login.html").user(headCashier.getUsername()).password("password"))
                .andExpect(authenticated().withUsername(headCashier.getUsername()))
                .andExpect(authenticated().withAuthorities(utilPopulate.getAuthorities(headCashier)));
    }

    @Test
    public void givenWrongLoginPassword_whenLogin_thenRedirectToLoginErrorPage() throws Exception {
        mockMvc.perform(formLogin("/login.html").user("wrongUsername").password("password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login-error.html"));
    }

}
