package com.app.bank_acquiring.integrationTest;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.ShopService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ShopService shopService;

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
        productRepository.deleteAll();
    }

    @Test
    public void givenAdminAccount_whenGetProducts_thenResponsesStatusOk() throws Exception {
        //creating shop owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        MvcResult res = mockMvc.perform(get("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(product.getName()));
    }

    @Test
    public void givenHeadCashierAccount_whenGetProducts_thenResponsesStatusOk() throws Exception {
        //creating shop owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        //creating employee for shop
        Account headCashier = createDetachedUser(Authority.HEAD_CASHIER);
        accountService.createEmployee(headCashier, new AccountInfo(), shop);

        MvcResult res = mockMvc.perform(get("/products")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(getAuthorities(headCashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(product.getName()));
    }

    @Test
    public void givenCashierAccount_whenGetProducts_thenResponsesStatusIsForbidden() throws Exception {
        //creating shop owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating employee
        Account cashier = createDetachedUser(Authority.CASHIER);
        accountService.createEmployee(cashier, new AccountInfo(), shop);

        mockMvc.perform(get("/products")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenGetExcelFile_thenResponseContainsAttachmentHeader() throws Exception {
        //creating shop owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        mockMvc.perform(get("/shops/" + shop.getId() + "/products/file")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products_" + shop.getId() + ".xlsx"));
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

    private Product createProductForShopInRepository(Shop shop) {
        Product product = new Product();
        product.setShop(shop);
        product.setName("product");
        product.setType(Type.ITEM);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        return productRepository.save(product);
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }

}
