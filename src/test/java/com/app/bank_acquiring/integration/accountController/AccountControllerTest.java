package com.app.bank_acquiring.integration.accountController;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.integration.UtilPopulate;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.IdValidationException;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
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
    public void givenAdminAccount_whenGetAccounts_thenResponsesStatusOk() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(get("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts"));
    }

    @Test
    public void givenCashierAccount_whenGetAccounts_thenResponsesStatusForbidden() throws Exception {
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        mockMvc.perform(get("/accounts")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenHeadCashierAccount_whenGetAccounts_thenResponsesStatusForbidden() throws Exception {
        Account headCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        mockMvc.perform(get("/accounts")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(headCashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenAnonymous_whenGetAccounts_thenResponsesStatusRedirection() throws Exception {
        mockMvc.perform(get("/accounts")
                        .with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void givenAdminAccount_whenGetAccounts_thenResponseContainsEmployees() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        MvcResult res = mockMvc.perform(get("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(view().name("accounts"))
                .andReturn();

        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(employee.getUsername()));
    }

    @Test
    public void givenAdminAccount_whenGetAccount_thenStatusOkAndResponseContainsEmployee() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        MvcResult res = mockMvc.perform(get("/accounts/" + employee.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(employee.getUsername()));
    }

    @Test
    public void givenNonAdminAccount_whenGetAccount_thenThrowsException() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(get("/accounts/" + admin.getId())
                .with(user(employee.getUsername()).password(employee.getPassword())
                        .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
    }

    @Test
    public void givenAdminAccount_whenGetCurrentAccount_thenStatusOkAndResponseContainsAccount() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);//createUserInRepository(Authority.ADMIN);
        MvcResult res = mockMvc.perform(get("/accounts/current")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(admin.getUsername()));
    }

    @Test
    public void givenNonAdminAccount_whenGetCurrentAccount_thenStatusOkAndResponseContainsAccount() throws Exception {
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        Account headCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);

        MvcResult res = mockMvc.perform(get("/accounts/current")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(cashier.getUsername()));

        res = mockMvc.perform(get("/accounts/current")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(headCashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        content = res.getResponse().getContentAsString();
        assertTrue(content.contains(headCashier.getUsername()));
    }

    @Test
    @Transactional
    public void whenCreateEmployee_thenRedirectToAccountsPageAndCreatesAccountInDb() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        mockMvc.perform(post("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", employee)
                        .flashAttr("accountInfo", accountInfo)
                        .param("shop", shop.getId() + ""))
                .andExpect(redirectedUrl("/accounts"));

        assertNotNull(accountRepository.findByUsername(employee.getUsername()));
        assertTrue(shopRepository.getOne(shop.getId()).getAccounts().size() == 2);
        assertTrue(accountInfoRepository.getOne(employee.getId()).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));
    }

    @Test
    public void givenExistingUsername_whenCreateEmployee_thenReturnAccountsPageWithBindingError() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        accountService.createEmployee(employee, accountInfo, shop);

        mockMvc.perform(post("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", employee)
                        .flashAttr("accountInfo", accountInfo)
                        .param("shop", shop.getId() + ""))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "username"))
                .andExpect(view().name("accounts"));
    }

    @Test
    public void whenUpdateAccount_thenRedirectAndUpdatesAccountInfoInDb() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        accountService.createEmployee(employee, accountInfo, shop);

        mockMvc.perform(post("/accounts/" + employee.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("accountInfo", accountInfo)
                        .param("shop", shop.getId() + "")
                        .param("authority", Authority.HEAD_CASHIER + ""))
                .andExpect(redirectedUrl("/accounts/" + employee.getId()));
        assertTrue(accountRepository.findByUsername(employee.getUsername()).getAuthority() == Authority.HEAD_CASHIER);
        assertTrue(accountInfoRepository.findByAccount(employee).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));
    }

    @Test
    public void whenUpdateCurrentAccount_thenRedirectAndUpdatesAccountInfoInDb() throws Exception {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");

        Account admin = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(post("/accounts/current")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("accountInfo", accountInfo))
                .andExpect(redirectedUrl("/accounts/current"));
        assertTrue(accountInfoRepository.findByAccount(admin).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));

        Account employee = utilPopulate.createUser(Authority.CASHIER);
        mockMvc.perform(post("/accounts/current")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        .flashAttr("accountInfo", accountInfo))
                .andExpect(redirectedUrl("/accounts/current"));
        assertTrue(accountInfoRepository.findByAccount(employee).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));
    }

    @Test
    public void whenChangeCurrentAccountPassword_thenUpdatesAccountPasswordInDb() throws Exception {
        String newPassword = "newPassword";

        Account admin = utilPopulate.createUser(Authority.ADMIN);//createUserInRepository(Authority.ADMIN);
        admin.setPassword("password");//to pass decoded pwd into controller as part of model
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(redirectedUrl("/login"));
        String accountPassword = accountRepository.findByUsername(admin.getUsername()).getPassword();
        assertTrue(passwordEncoder.matches(newPassword, accountPassword));

        Account employee = utilPopulate.createUser(Authority.CASHIER);
        employee.setPassword("password");//to pass into controller as part of model
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(redirectedUrl("/login"));
        accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertTrue(passwordEncoder.matches(newPassword, accountPassword));
    }


    @Test
    public void givenIncorrectExistingPassword_whenChangeCurrentAccountPassword_thenReturnAccountPageWithBindingError() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        admin.setPassword("incorrectPassword");//to pass into controller as part of model
        String newPassword = "newPassword";
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(admin.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
    }

    @Test
    public void givenPasswordMismatch_whenChangeCurrentAccountPassword_thenReturnAccountPageWithBindingError() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        String newPassword = "newPassword";

        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", "passwordMismatch"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(admin.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
    }

    @Test
    public void givenInvalidNewPassword_whenChangeCurrentAccountPassword_thenReturnAccountPageWithBindingError() throws Exception {
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        String newPassword = "newPassword";

        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", " ")
                        .param("repeatPwd", " "))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(admin.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
    }

    @Test
    public void whenChangeAccountPassword_thenUpdatesAccountPasswordInDb() throws Exception {
        String newPassword = "newPassword";

        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(redirectedUrl("/accounts/" + employee.getId()));
        String accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertTrue(passwordEncoder.matches(newPassword, accountPassword));
    }

    @Test
    public void givenPasswordMismatch_whenChangeAccountPassword_thenReturnsAccountPageWithBindingError() throws Exception {
        String newPassword = "newPassword";

        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", "mismatch"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
    }

    @Test
    public void givenInvalidNewPassword_whenChangeAccountPassword_thenReturnsAccountPageWithBindingError() throws Exception {
        String newPassword = "newPassword";

        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Account employee = utilPopulate.createDetachedUser(Authority.CASHIER);

        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", " ")
                        .param("repeatPwd", " "))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
    }


}
