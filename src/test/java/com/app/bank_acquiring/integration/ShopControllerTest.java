package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

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
    private ShopRepository shopRepository;
    @Autowired
    private ShopService shopService;
    @Autowired
    private UtilPopulate utilPopulate;


    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        utilPopulate.clearTables();
    }

    @After
    public void tearDown() {
        utilPopulate.clearTables();
    }

    @Test
    public void givenAdminAccount_whenGetShops_thenResponsesStatusOk() throws Exception {
        //creating shop with owner/admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);

        MvcResult res = mockMvc.perform(get("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("shops"))
                .andReturn();
        //response should contain info about shops
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(shop.getName()));
    }

    @Test
    public void givenNonAdmin_whenGetShops_thenResponsesStatusIsForbidden() throws Exception {
        //creating shop with owner/admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //creating employees for the shop
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        Account hCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, cashier.getUsername());
        shopService.bundleShopWithAccount(shop, hCashier.getUsername());
        //checking whether these employees have access to the shop
        mockMvc.perform(get("/shops")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/shops")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(hCashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenCreateShop_thenNewShopIsSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        //creating not persisted shop
        Shop detachedShop = utilPopulate.createDetachedShop();
        //confirming that there aren't shops in repo as it should be
        assertEquals(0, shopRepository.findAll().size());

        mockMvc.perform(post("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending shop to be persisted
                        .flashAttr("shop", detachedShop))
                .andExpect(redirectedUrl("/shops"));
        //new shop should be saved in repository
        List<Shop> shops = shopRepository.findAll();
        assertEquals(1, shops.size());
        assertEquals(shops.get(0).getName(), detachedShop.getName());
    }

    @Test
    public void givenNamelessShop_whenCreateShop_thenNewShopNotSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        //creating not persisted shop
        Shop detachedShop = utilPopulate.createDetachedShop();
        detachedShop.setName("");
        //confirming that there aren't shops in repo as it should be
        assertEquals(0, shopRepository.findAll().size());

        mockMvc.perform(post("/shops")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending shop to be persisted
                        .flashAttr("shop", detachedShop))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("shop", "name"))
                .andExpect(view().name("shops"));
        //shop is not persisted in db due to constraint(name is mandatory for shop!!)
        assertEquals(0, shopRepository.findAll().size());
    }

    @Test
    public void whenDeleteShop_thenShopDeletedFromRepository() throws Exception {
        //creating shop with owner
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //confirming that there is one shop in repo as it should be
        assertEquals(1, shopRepository.findAll().size());

        MvcResult res = mockMvc.perform(delete("/shops/" + shop.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(redirectedUrl("/shops"))
                .andReturn();
        //response should not contain info about deleted shop
        assertFalse(res.getResponse().getContentAsString().contains(shop.getName()));
        assertEquals(0, shopRepository.findAll().size());
    }


    @Test
    public void givenWrongShopId_whenDeleteShop_thenThrowsException() throws Exception {
        //creating two shop owners
        Account admin1 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop1 = utilPopulate.createShopForAdmin(admin1);
        Account admin2 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop2 = utilPopulate.createShopForAdmin(admin2);
        //confirming that there is two shops in repo as it should be
        assertEquals(2, shopRepository.findAll().size());
        //should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop1.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
    }

    @Test
    public void whenDeleteAccount_thenDeletesAccountFromRepository() throws Exception {
        //creating shop with owner/admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //creating employee for the shop
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, cashier.getUsername());
        //confirming that there are two accounts in repo
        assertEquals(2, accountRepository.findAll().size());
        //deleting cashier
        mockMvc.perform(delete("/shops/" + shop.getId() + "/accounts/" + cashier.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(redirectedUrl("/accounts"));
        //cashier acc should be deleted from db
        assertNull(accountRepository.findByUsername(cashier.getUsername()));
        assertEquals(1, accountRepository.findAll().size());
    }

    @Test
    public void givenWrongIds_whenDeleteAccount_thenThrowsException() throws Exception {
        //creating two shops with owners/admins
        Account admin1 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop1 = utilPopulate.createShopForAdmin(admin1);
        Account admin2 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop2 = utilPopulate.createShopForAdmin(admin2);
        //creating employee for the shop1
        Account cashier1 = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop1, cashier1.getUsername());
        //confirming that there are two accounts in repo
        assertEquals(3, accountRepository.findAll().size());
        //sending existing but wrong shop -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop2.getId() + "/accounts/" + cashier1.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
        //sending non-existing shop -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + 500 + "/accounts/" + cashier1.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
        //sending existing shop, but non-existing user -> should throw exception due to ids validation
        mockMvc.perform(delete("/shops/" + shop2.getId() + "/accounts/" + 500)
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException));
    }

}
