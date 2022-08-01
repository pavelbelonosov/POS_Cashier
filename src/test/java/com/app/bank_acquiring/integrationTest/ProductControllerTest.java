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
import org.springframework.cache.CacheManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
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
    @Autowired
    CacheManager cacheManager;


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
    public void testCashing() {

    }

    private Optional<Product> getCachedProduct(Long id) {

        return ofNullable(cacheManager.getCache("products")).map(c -> c.get(id, Product.class));
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


    @Test
    public void givenAdminUser_whenUpdateBalance_thenProductBalanceIsUpdatedInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product1 = createProductForShopInRepository(shop);
        Product product2 = createProductForShopInRepository(shop);

        assertTrue(product1.getBalance() == 0);
        assertTrue(product2.getBalance() == 0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending products ids that we want to update
                        .param("prods", new String[]{product1.getId() + "", product2.getId() + ""})
                        //sending new balance values
                        .param("balances", new String[]{"10", "20"}))
                .andExpect(redirectedUrl("/products"));

        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 10);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 20);
    }

    @Test
    public void givenHeadCashierUser_whenUpdateBalance_thenProductBalanceIsUpdatedInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account hCashier = createUserInRepository(Authority.HEAD_CASHIER);
        Shop shop = createShopForAdminInRepository(hCashier);
        Product product1 = createProductForShopInRepository(shop);
        Product product2 = createProductForShopInRepository(shop);

        assertTrue(product1.getBalance() == 0);
        assertTrue(product2.getBalance() == 0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(getAuthorities(hCashier)))
                        //sending products ids that we want to update
                        .param("prods", new String[]{product1.getId() + "", product2.getId() + ""})
                        //sending new balance values
                        .param("balances", new String[]{"10", "20"}))
                .andExpect(redirectedUrl("/products"));

        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 10);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 20);
    }

    @Test
    public void givenCashierUser_whenUpdateBalance_thenStatusIsForbidden() throws Exception {
        //creating shop owner and products for the shop
        Account cashier = createUserInRepository(Authority.CASHIER);
        Shop shop = createShopForAdminInRepository(cashier);
        Product product1 = createProductForShopInRepository(shop);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier)))
                        //sending products ids that we want to update
                        .param("prods", new String[]{product1.getId() + ""})
                        //sending new balance values
                        .param("balances", new String[]{"10"}))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenNotEqualAmountsOfIdsAndBalanceVAlues_whenUpdateBalance_thenNotUpdateInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product1 = createProductForShopInRepository(shop);
        Product product2 = createProductForShopInRepository(shop);

        assertTrue(product1.getBalance() == 0);
        assertTrue(product2.getBalance() == 0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending products ids that we want to update
                        .param("prods", new String[]{product1.getId() + "", product2.getId() + ""})
                        //sending new balance values
                        .param("balances", new String[]{"10"}))
                .andExpect(redirectedUrl("/products"));

        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 0);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 0);
    }

    @Test
    public void whenCopyProducts_thenNewProductsSavedInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop1 = createShopForAdminInRepository(admin);
        Shop shop2 = createShopForAdminInRepository(admin);
        Product product1 = createProductForShopInRepository(shop1);
        Product product2 = createProductForShopInRepository(shop1);
        //confirming that there are only two products in repo as it should be
        assertTrue(productRepository.findAll().size() == 2);
        mockMvc.perform(post("/shops/" + shop1.getId() + "/products/copy")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending products ids that we want to copy to other shop
                        .param("prods", new String[]{product1.getId() + "", product2.getId() + ""})
                        //sending shop id to where we copy products
                        .param("targetShopId", shop2.getId() + ""))
                .andExpect(redirectedUrl("/products"));

        List<Product> products = productRepository.findAll();
        assertTrue(products.size() == 4);
        assertTrue(products.stream().filter(product -> product.getShop().equals(shop2)).count() == 2);
    }

    @Test
    public void whenCreateProduct_thenNewProductIsSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating product not persisted yet
        Product detachedProduct = createDetachedProduct(Type.ITEM);
        //confirming that there aren't products in repo as it should be
        assertTrue(productRepository.findAll().size() == 0);

        mockMvc.perform(post("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending product to be persisted
                        .flashAttr("product", detachedProduct)
                        //sending shop id to which product should be belonged
                        .param("shop", shop.getId() + ""))
                .andExpect(redirectedUrl("/products"));

        List<Product> products = productRepository.findAll();
        assertTrue(products.size() == 1);
        assertTrue(products.get(0).getShop().equals(shop));
    }

    @Test
    public void givenNullShop_whenCreateProduct_thenReturnProductsPageWithBindingError() throws Exception {
        //creating admin user
        Account admin = createUserInRepository(Authority.ADMIN);
        //creating product not persisted yet
        Product detachedProduct = createDetachedProduct(Type.ITEM);
        //confirming that there aren't products in repo as it should be
        assertTrue(productRepository.findAll().size() == 0);

        mockMvc.perform(post("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending product to be persisted
                        .flashAttr("product", detachedProduct)
                        //sending shop id to which product should be belonged
                        .param("shop", ""))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("product", "shop"))
                .andExpect(view().name("products"));
        assertTrue(productRepository.findAll().size() == 0);
    }

    @Test
    public void givenProductWithoutName_whenCreateProduct_thenReturnProductsPageWithBindingError() throws Exception {
        //creating admin user with shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating product not persisted yet
        Product detachedProduct = createDetachedProduct(Type.ITEM);
        detachedProduct.setName(" ");
        //confirming that there aren't products in repo as it should be
        assertTrue(productRepository.findAll().size() == 0);

        mockMvc.perform(post("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending product to be persisted
                        .flashAttr("product", detachedProduct)
                        //sending shop id to which product should be belonged
                        .param("shop", shop.getId() + ""))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("product", "name"))
                .andExpect(view().name("products"));
        assertTrue(productRepository.findAll().size() == 0);
    }

    @Test
    public void givenProductWithTypeService_whenCreateProduct_thenNewProdIsSavedInRepository() throws Exception {
        //creating admin user with shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating product not persisted yet
        Product detachedProduct = createDetachedProduct(Type.SERVICE);
        detachedProduct.setMeasurementUnit(null);
        //confirming that there aren't products in repo as it should be
        assertTrue(productRepository.findAll().size() == 0);

        mockMvc.perform(post("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending product to be persisted
                        .flashAttr("product", detachedProduct)
                        //sending shop id to which product should be belonged
                        .param("shop", shop.getId() + ""))
                .andExpect(redirectedUrl("/products"));

        List<Product> products = productRepository.findAll();
        assertTrue(products.size() == 1);
        Product persistedProduct = products.get(0);
        assertTrue(persistedProduct.getShop().equals(shop));
        assertTrue(persistedProduct.getBalance() == Integer.MAX_VALUE);
        assertTrue(persistedProduct.getMeasurementUnit() == MeasurementUnit.UNIT);
    }

    @Test
    public void whenDeleteProductById_thenProductDeletedFromRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        //confirming that there is one product in repo as it should be
        assertTrue(productRepository.findAll().size() == 1);

        mockMvc.perform(delete("/products/" + product.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(redirectedUrl("/products"));

        assertTrue(productRepository.findAll().size() == 0);
    }

    @Test
    public void whenDeleteMany_thenProductsDeletedFromRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product1 = createProductForShopInRepository(shop);
        Product product2 = createProductForShopInRepository(shop);
        //confirming that there are two products in repo as it should be
        assertTrue(productRepository.findAll().size() == 2);

        mockMvc.perform(post("/products/deleteMany")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .param("prods", new String[]{product1.getId() + "", product2.getId() + ""}))
                .andExpect(redirectedUrl("/products"));

        assertTrue(productRepository.findAll().size() == 0);
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

    private Product createDetachedProduct(Type type) {
        Product product = new Product();
        product.setName("product");
        product.setType(type);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        return product;
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }

}
