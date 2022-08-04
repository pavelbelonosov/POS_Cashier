package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.IdValidationException;
import com.app.bank_acquiring.service.ShopService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
public class ShopControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopService shopService;


    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
    }

    @After
    public void tearDown() {
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
    }

    @Test
    public void givenAdminAccount_whenGetShops_thenResponsesStatusOk() throws Exception {
        //creating shop with owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);

        MvcResult res = mockMvc.perform(get("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("shops"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(shop.getName()));
    }

    @Test
    public void givenNonAdmin_whenGetShops_thenResponsesStatusIsForbidden() throws Exception {
        //creating shop with owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating employees for the shop
        Account cashier = createUserInRepository(Authority.CASHIER);
        Account hCashier = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, cashier.getUsername());
        shopService.bundleShopWithAccount(shop, hCashier.getUsername());
        //checking whether employees have access to the shops
        mockMvc.perform(get("/shops")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/shops")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(getAuthorities(hCashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenCreateShop_thenNewShopIsSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = createUserInRepository(Authority.ADMIN);
        //creating not persisted shop
        Shop detachedShop = createDetachedShop();
        //confirming that there aren't shops in repo as it should be
        assertTrue(shopRepository.findAll().size() == 0);

        mockMvc.perform(post("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending shop to be persisted
                        .flashAttr("shop", detachedShop))
                .andExpect(redirectedUrl("/shops"));

        List<Shop> shops = shopRepository.findAll();
        assertTrue(shops.size() == 1);
        assertTrue(shops.get(0).getName().equals(detachedShop.getName()));
    }

    @Test
    public void givenNamelessShop_whenCreateShop_thenNewShopNotSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = createUserInRepository(Authority.ADMIN);
        //creating not persisted shop
        Shop detachedShop = createDetachedShop();
        detachedShop.setName("");
        //confirming that there aren't shops in repo as it should be
        assertTrue(shopRepository.findAll().size() == 0);

        mockMvc.perform(post("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending shop to be persisted
                        .flashAttr("shop", detachedShop))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("shop", "name"))
                .andExpect(view().name("shops"));
        assertTrue(shopRepository.findAll().size() == 0);
    }

    @Test
    public void whenDeleteShop_thenShopDeletedFromRepository() throws Exception {
        //creating shop with owner
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //confirming that there is one shop in repo as it should be
        assertTrue(shopRepository.findAll().size() == 1);

        MvcResult res = mockMvc.perform(delete("/shops/" + shop.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(redirectedUrl("/shops"))
                .andReturn();
        assertFalse(res.getResponse().getContentAsString().contains(shop.getName()));
        assertTrue(shopRepository.findAll().size() == 0);
    }


    @Test
    public void givenWrongShopId_whenDeleteShop_thenThrowsException() throws Exception {
        //creating two shop owners
        Account admin1 = createUserInRepository(Authority.ADMIN);
        Shop shop1 = createShopForAdminInRepository(admin1);
        Account admin2 = createUserInRepository(Authority.ADMIN);
        Shop shop2 = createShopForAdminInRepository(admin2);
        //confirming that there is two shops in repo as it should be
        assertTrue(shopRepository.findAll().size() == 2);
        //should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop1.getId())
                .with(user(admin2.getUsername()).password(admin2.getPassword())
                        .authorities(getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
    }

    @Test
    public void whenDeleteAccount_thenDeletesAccountFromRepository() throws Exception {
        //creating shop with owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating employee for the shop
        Account cashier = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, cashier.getUsername());
        //confirming that there are two accs in repo
        System.out.println(accountRepository.findAll().size());
        assertTrue(accountRepository.findAll().size() == 2);

        mockMvc.perform(delete("/shops/" + shop.getId() + "/accounts/" + cashier.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(redirectedUrl("/accounts"));
        assertNull(accountRepository.findByUsername(cashier.getUsername()));
        assertTrue(accountRepository.findAll().size() == 1);
    }

    @Test
    public void givenWrongIds_whenDeleteAccount_thenThrowsException() throws Exception {
        //creating two shops with owners/admins

        Account admin1 = createUserInRepository(Authority.ADMIN);
        Shop shop1 = createShopForAdminInRepository(admin1);
        Account admin2 = createUserInRepository(Authority.ADMIN);
        Shop shop2 = createShopForAdminInRepository(admin2);
        //creating employee for the shop1
        Account cashier1 = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop1, cashier1.getUsername());
        //confirming that there are two accs in repo
        System.out.println(accountRepository.findAll().size());
        assertTrue(accountRepository.findAll().size() == 3);
        //sending existing but wrong shop -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop2.getId() + "/accounts/" + cashier1.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
        //sending non-existing shop -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + 500 + "/accounts/" + cashier1.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
        //sending existing shop, but non-existing user -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop2.getId() + "/accounts/" + 500)
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
    }



    private Account createUserInRepository(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setAuthority(authority);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccount(user);
        accountInfoRepository.save(accountInfo);
        return accountRepository.save(user);
    }

    private Account createDetachedUser(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setAuthority(authority);
        return user;
    }

    private Shop createShopForAdminInRepository(Account admin) {
        Shop shop = new Shop();
        shop.setName("shop");
        List<Account> accountList = new ArrayList<>();
        accountList.add(admin);
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }

    private Shop createDetachedShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        return shop;
    }


    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }
}
