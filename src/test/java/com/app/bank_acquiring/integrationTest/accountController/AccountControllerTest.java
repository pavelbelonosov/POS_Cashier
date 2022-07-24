package com.app.bank_acquiring.integrationTest.accountController;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import org.junit.After;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @After
    public void tearDown() {
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
    }

    @Test
    public void givenAdminAccount_whenGetAccounts_thenResponsesStatusOk() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        mockMvc.perform(get("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts"));
    }

    @Test
    public void givenCashierAccount_whenGetAccounts_thenResponsesStatusForbidden() throws Exception {
        Account cashier = createUserInRepository(Authority.CASHIER);
        mockMvc.perform(get("/accounts")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenHeadCashierAccount_whenGetAccounts_thenResponsesStatusForbidden() throws Exception {
        Account headCashier = createUserInRepository(Authority.HEAD_CASHIER);
        mockMvc.perform(get("/accounts")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(getAuthorities(headCashier))))
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
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        MvcResult res = mockMvc.perform(get("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(view().name("accounts"))
                .andReturn();

        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(employee.getUsername()));
    }

    @Test
    public void givenAdminAccount_whenGetAccount_thenStatusOkAndResponseContainsEmployee() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        MvcResult res = mockMvc.perform(get("/accounts/" + employee.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(employee.getUsername()));
    }

    @Test(expected = NestedServletException.class)
    public void givenNonAdminAccount_whenGetAccount_thenThrowsException() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(get("/accounts/" + admin.getId())
                .with(user(employee.getUsername()).password(employee.getPassword())
                        .authorities(getAuthorities(employee))));
    }

    @Test
    public void givenAdminAccount_whenGetCurrentAccount_thenStatusOkAndResponseContainsAccount() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        MvcResult res = mockMvc.perform(get("/accounts/current")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(admin.getUsername()));
    }

    @Test
    public void givenNonAdminAccount_whenGetCurrentAccount_thenStatusOkAndResponseContainsAccount() throws Exception {
        Account cashier = createUserInRepository(Authority.CASHIER);
        Account headCashier = createUserInRepository(Authority.HEAD_CASHIER);

        MvcResult res = mockMvc.perform(get("/accounts/current")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(cashier.getUsername()));

        res = mockMvc.perform(get("/accounts/current")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(getAuthorities(headCashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andReturn();
        content = res.getResponse().getContentAsString();
        assertTrue(content.contains(headCashier.getUsername()));
    }

    @Test
    @Transactional
    public void whenCreateEmployee_thenRedirectToAccountsPageAndCreatesAccountInDb() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        mockMvc.perform(post("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        accountService.createEmployee(employee, accountInfo, shop);

        mockMvc.perform(post("/accounts")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("account", employee)
                        .flashAttr("accountInfo", accountInfo)
                        .param("shop", shop.getId() + ""))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "username"))
                .andExpect(view().name("accounts"));
    }

    @Test
    public void whenUpdateAccount_thenRedirectAndUpdatesAccountInfoInDb() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setTelephoneNumber("9876543210");
        accountService.createEmployee(employee, accountInfo, shop);

        mockMvc.perform(post("/accounts/" + employee.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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

        Account admin = createUserInRepository(Authority.ADMIN);
        mockMvc.perform(post("/accounts/current")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("accountInfo", accountInfo))
                .andExpect(redirectedUrl("/accounts/current"));
        assertTrue(accountInfoRepository.findByAccount(admin).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));

        Account employee = createUserInRepository(Authority.CASHIER);
        mockMvc.perform(post("/accounts/current")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        .flashAttr("accountInfo", accountInfo))
                .andExpect(redirectedUrl("/accounts/current"));
        assertTrue(accountInfoRepository.findByAccount(employee).getTelephoneNumber().equals(accountInfo.getTelephoneNumber()));
    }

    @Test
    public void whenChangeCurrentAccountPassword_thenUpdatesAccountPasswordInDb() throws Exception {
        String newPassword = "newPassword";

        Account admin = createUserInRepository(Authority.ADMIN);
        admin.setPassword("password");//to pass into controller as part of model
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(redirectedUrl("/accounts/current"));
        String accountPassword = accountRepository.findByUsername(admin.getUsername()).getPassword();
        assertTrue(passwordEncoder.matches(newPassword, accountPassword));

        Account employee = createUserInRepository(Authority.CASHIER);
        employee.setPassword("password");//to pass into controller as part of model
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        .flashAttr("account", admin)
                        .param("newPassword", newPassword)
                        .param("repeatPwd", newPassword))
                .andExpect(redirectedUrl("/accounts/current"));
        accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertTrue(passwordEncoder.matches(newPassword, accountPassword));
    }


    @Test
    public void givenIncorrectExistingPassword_whenChangeCurrentAccountPassword_thenReturnAccountPageWithBindingError() throws Exception {
        Account admin = createUserInRepository(Authority.ADMIN);
        admin.setPassword("incorrectPassword");//to pass into controller as part of model
        String newPassword = "newPassword";
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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
        Account admin = createUserInRepository(Authority.ADMIN);
        admin.setPassword("incorrectPassword");//to pass into controller as part of model
        String newPassword = "newPassword";
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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
        Account admin = createUserInRepository(Authority.ADMIN);
        admin.setPassword("password");//to pass into controller as part of model
        String newPassword = "newPassword";
        mockMvc.perform(post("/accounts/current/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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

        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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

        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
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

        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdmin(admin);
        Account employee = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(employee, new AccountInfo(), shop);
        mockMvc.perform(post("/accounts/" + employee.getId() + "/newpwd")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("account", admin)
                        .param("newPassword", " ")
                        .param("repeatPwd", " "))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("account", "password"))
                .andExpect(view().name("account"));
        String accountPassword = accountRepository.findByUsername(employee.getUsername()).getPassword();
        assertFalse(passwordEncoder.matches(newPassword, accountPassword));
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

    private Account createDetachedUser(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(passwordEncoder.encode("password"));
        user.setAuthority(authority);
        return user;
    }

    private Shop createShopForAdmin(Account admin) {
        Shop shop = new Shop();
        shop.setName("shop");
        List<Account> accountList = new ArrayList<>();
        accountList.add(admin);
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }

}
