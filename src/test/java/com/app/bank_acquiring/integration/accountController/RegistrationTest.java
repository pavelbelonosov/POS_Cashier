package com.app.bank_acquiring.integration.accountController;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.integration.UtilPopulate;
import com.app.bank_acquiring.repository.AccountRepository;
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

import java.util.Random;

import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RegistrationTest {


    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
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
    public void whenGetRegistrationForm_thenStatusOk() throws Exception {
        mockMvc.perform(get("/accounts/registration")
                        .with(anonymous()))
                .andExpect(status().isOk());
    }

    @Test
    public void whenCreateAdminUser_thenRedirectToLoginPageAndCreatesAccountInDB() throws Exception {
        Account admin = new Account();
        admin.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        admin.setPassword("password");
        mockMvc.perform(post("/accounts/registration")
                        .flashAttr("account", admin)
                        .flashAttr("accountInfo", new AccountInfo())
                        .param("repeatPWD", admin.getPassword()))
                .andExpect(view().name("redirect:/login"));

        assertNotNull(accountRepository.findByUsername(admin.getUsername()));
    }

    @Test
    public void givenPasswordMismatch_whenCreateAdminUser_thenReturnsBindingError() throws Exception {
        Account admin = new Account();
        admin.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        admin.setPassword("password");
        mockMvc.perform(post("/accounts/registration")
                        .flashAttr("account", admin)
                        .flashAttr("accountInfo", new AccountInfo())
                        .param("repeatPWD", "notMatchingPwd"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("registration"));

        assertNull(accountRepository.findByUsername(admin.getUsername()));
    }

    @Test
    public void givenExistingUsername_whenCreateAdminUser_thenReturnsBindingError() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(post("/accounts/registration")
                        .flashAttr("account", admin)
                        .flashAttr("accountInfo", new AccountInfo())
                        .param("repeatPWD", admin.getPassword()))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "username"))
                .andExpect(view().name("registration"));
    }

    @Test
    public void givenInvalidUsernamePasswordEmail_whenCreateAdminUser_thenReturnsBindingErrors() throws Exception {
        Account admin = new Account();
        admin.setUsername(" ");
        admin.setPassword("");
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setEmail(" ");
        mockMvc.perform(post("/accounts/registration")
                        .flashAttr("account", admin)
                        .flashAttr("accountInfo", accountInfo)
                        .param("repeatPWD", admin.getPassword()))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "username"))
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(model().attributeHasFieldErrors("accountInfo", "email"))
                .andExpect(view().name("registration"));
    }

}
