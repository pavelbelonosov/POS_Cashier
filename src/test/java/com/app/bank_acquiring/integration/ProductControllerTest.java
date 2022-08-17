package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.ShopService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.junit.Assert.*;
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
    private ProductRepository productRepository;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UtilPopulate utilPopulate;
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
        utilPopulate.clearTables();
    }

    @Test
    public void givenAdminAccount_whenGetProducts_thenResponsesStatusOk() throws Exception {
        //creating admin with shop, and product for shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);

        MvcResult res = mockMvc.perform(get("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andReturn();
        //response should contain info about existing product
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(product.getName()));
    }

    @Test
    public void givenHeadCashierAccount_whenGetProducts_thenResponsesStatusOk() throws Exception {
        //creating admin with shop, and product for shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        //creating employee for shop
        Account headCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, headCashier.getUsername());

        MvcResult res = mockMvc.perform(get("/products")
                        .with(user(headCashier.getUsername()).password(headCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(headCashier))))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        //response should contain product info
        assertTrue(content.contains(product.getName()));
    }

    @Test
    public void givenCashierAccount_whenGetProducts_thenResponsesStatusIsForbidden() throws Exception {
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        mockMvc.perform(get("/products")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenGetExcelFile_thenResponseContainsAttachmentHeader() throws Exception {
        //creating admin with shop, and product for shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        utilPopulate.createProductForShop(shop);

        mockMvc.perform(get("/shops/" + shop.getId() + "/products/file")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products_" + shop.getId() + ".xlsx"));
    }


    @Test
    public void givenAdminUser_whenUpdateBalance_thenProductBalanceIsUpdatedInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product1 = utilPopulate.createProductForShop(shop);
        Product product2 = utilPopulate.createProductForShop(shop);
        //product balances should be nul at the beginning
        assertEquals(0, product1.getBalance(), 0.0);
        assertEquals(0, product2.getBalance(), 0.0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending products ids that we want to update
                        .param("prods", product1.getId() + "", product2.getId() + "")
                        //sending new balance values
                        .param("balances", "10", "20"))
                .andExpect(redirectedUrl("/products"));
        //product balances should be updated respectively
        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 10);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 20);
    }

    @Test
    public void givenHeadCashierUser_whenUpdateBalance_thenProductBalanceIsUpdatedInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account hCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        Shop shop = utilPopulate.createShopForAdmin(hCashier);
        Product product1 = utilPopulate.createProductForShop(shop);
        Product product2 = utilPopulate.createProductForShop(shop);
        //product balances should be nul at the beginning
        assertEquals(0, product1.getBalance(), 0.0);
        assertEquals(0, product2.getBalance(), 0.0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(hCashier)))
                        //sending products ids that we want to update
                        .param("prods", product1.getId() + "", product2.getId() + "")
                        //sending new balance values
                        .param("balances", "10", "20"))
                .andExpect(redirectedUrl("/products"));
        //product balances should be updated respectively
        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 10);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 20);
    }


    @Test
    public void givenCashierUser_whenUpdateBalance_thenStatusIsForbidden() throws Exception {
        //creating shop owner and products for the shop
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        Shop shop = utilPopulate.createShopForAdmin(cashier);
        Product product1 = utilPopulate.createProductForShop(shop);

        mockMvc.perform(post("/products/updateBalance")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier)))
                        //sending products ids that we want to update
                        .param("prods", product1.getId() + "")
                        //sending new balance values
                        .param("balances", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenNotEqualAmountsOfIdsAndBalanceValues_whenUpdateBalance_thenNotUpdateInRepository() throws Exception {
        //creating shop owner and products for the shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product1 = utilPopulate.createProductForShop(shop);
        Product product2 = utilPopulate.createProductForShop(shop);
        //product balances should be nul at the beginning
        assertEquals(0, product1.getBalance(), 0.0);
        assertEquals(0, product2.getBalance(), 0.0);
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending products ids that we want to update
                        .param("prods", product1.getId() + "", product2.getId() + "")
                        //sending new balance values(one value for two product!)
                        .param("balances", "10"))
                .andExpect(redirectedUrl("/products"));
        //product balances should remain nul due to incorrect request
        assertTrue(productRepository.findById(product1.getId()).get().getBalance() == 0);
        assertTrue(productRepository.findById(product2.getId()).get().getBalance() == 0);
    }

    @Test
    public void whenUpdateBalance_thenPutUpdatedProductInSpringCache() throws Exception {
        //creating shop owner and products for the shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Long id = product.getId();
        //invoking cacheable method
        productService.getProduct(id, admin.getUsername());
        //check if this product is added into the cache with nil balance
        assertEquals(product, getCachedProduct(id));
        assertEquals(0, getCachedProduct(id).getBalance(), 0.0);
        //updating balance of cached product by invoking @CachePut on save method in ProductService
        mockMvc.perform(post("/products/updateBalance")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending products ids that we want to update
                        .param("prods", product.getId() + "")
                        //sending new balance values
                        .param("balances", "10"))
                .andExpect(redirectedUrl("/products"));
        //cached product should be updated with new balance
        assertEquals(10, getCachedProduct(id).getBalance(), 0.0);
    }


    @Test
    public void whenCopyProducts_thenNewProductsSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop1 = utilPopulate.createShopForAdmin(admin);
        Shop shop2 = utilPopulate.createShopForAdmin(admin);
        //saving products for 1-st shop
        Product product1 = utilPopulate.createProductForShop(shop1);
        Product product2 = utilPopulate.createProductForShop(shop1);
        //confirming that there are only two products in repo as it should be
        assertEquals(2, productRepository.findAll().size());
        //copying products request
        mockMvc.perform(post("/shops/" + shop1.getId() + "/products/copy")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending products ids that we want to copy to other shop
                        .param("prods", product1.getId() + "", product2.getId() + "")
                        //sending shop id to where we copy products
                        .param("targetShopId", shop2.getId() + ""))
                .andExpect(redirectedUrl("/products"));
        //two new products should be created in db
        List<Product> products = productRepository.findAll();
        assertEquals(4, products.size());
        //two products among all existing should belong to 2-nd shop
        assertEquals(2, products.stream().filter(product -> product.getShop().equals(shop2)).count());
    }

    @Test
    public void whenCreateProduct_thenNewProductIsSavedInRepository() throws Exception {
        //creating shop owner
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //creating product not persisted yet
        Product detachedProduct = utilPopulate.createDetachedProduct(Type.ITEM);
        //confirming that there aren't products in repo as it should be
        assertEquals(0, productRepository.findAll().size());

        mockMvc.perform(post("/products")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending product to be persisted
                        .flashAttr("product", detachedProduct)
                        //sending shop id to which product should be belonged
                        .param("shop", shop.getId() + ""))
                .andExpect(redirectedUrl("/products"));

        List<Product> products = productRepository.findAll();
        assertEquals(1, products.size());
        assertEquals(products.get(0).getShop(), shop);
    }


        @Test
        public void givenNullShop_whenCreateProduct_thenReturnProductsPageWithBindingError() throws Exception {
            //creating admin user
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            //creating product not persisted yet
            Product detachedProduct = utilPopulate.createDetachedProduct(Type.ITEM);
            //confirming that there aren't products in repo as it should be
            assertEquals(0, productRepository.findAll().size());

            mockMvc.perform(post("/products")
                            .with(user(admin.getUsername()).password(admin.getPassword())
                                    .authorities(utilPopulate.getAuthorities(admin)))
                            //sending product to be persisted
                            .flashAttr("product", detachedProduct)
                            //sending shop id to which product should be belonged
                            .param("shop", ""))
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeHasFieldErrors("product", "shop"))
                    .andExpect(view().name("products"));
            //Terminal should not be persisted due to binding error(indicating shop is mandatory when creating new product!)
            assertEquals(0, productRepository.findAll().size());
        }

       @Test
        public void givenProductWithoutName_whenCreateProduct_thenReturnProductsPageWithBindingError() throws Exception {
            //creating admin user with shop
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            Shop shop = utilPopulate.createShopForAdmin(admin);
            //creating product not persisted yet
            Product detachedProduct = utilPopulate.createDetachedProduct(Type.ITEM);
            detachedProduct.setName(" ");
            //confirming that there aren't products in repo as it should be
           assertEquals(0, productRepository.findAll().size());

            mockMvc.perform(post("/products")
                            .with(user(admin.getUsername()).password(admin.getPassword())
                                    .authorities(utilPopulate.getAuthorities(admin)))
                            //sending product to be persisted
                            .flashAttr("product", detachedProduct)
                            //sending shop id to which product should be belonged
                            .param("shop", shop.getId() + ""))
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeHasFieldErrors("product", "name"))
                    .andExpect(view().name("products"));
            //Terminal should not be persisted due to binding error()name for product is mandatory/constraint)
           assertEquals(0, productRepository.findAll().size());
        }

        @Test
        public void givenProductWithTypeService_whenCreateProduct_thenNewProdIsSavedInRepository() throws Exception {
            //creating admin user with shop
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            Shop shop = utilPopulate.createShopForAdmin(admin);
            //creating product not persisted yet
            Product detachedProduct = utilPopulate.createDetachedProduct(Type.SERVICE);
            detachedProduct.setMeasurementUnit(null);
            //confirming that there aren't products in repo as it should be
            assertEquals(0, productRepository.findAll().size());

            mockMvc.perform(post("/products")
                            .with(user(admin.getUsername()).password(admin.getPassword())
                                    .authorities(utilPopulate.getAuthorities(admin)))
                            //sending product to be persisted
                            .flashAttr("product", detachedProduct)
                            //sending shop id to which product should be belonged
                            .param("shop", shop.getId() + ""))
                    .andExpect(redirectedUrl("/products"));
            //new product should be saved in db and be belonged to the shop
            List<Product> products = productRepository.findAll();
            assertEquals(1, products.size());
            Product persistedProduct = products.get(0);
            assertEquals(persistedProduct.getShop(), shop);
            //Products of type-service has no balance available to users, for simplicity their balance set to max
            assertEquals(Integer.MAX_VALUE, persistedProduct.getBalance(), 0.0);
            //app logic internally should set measurement unit
            assertSame(persistedProduct.getMeasurementUnit(), MeasurementUnit.UNIT);
        }

        @Test
        public void whenDeleteProductById_thenProductDeletedFromRepository() throws Exception {
            //creating shop owner and products for the shop
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            Shop shop = utilPopulate.createShopForAdmin(admin);
            Product product = utilPopulate.createProductForShop(shop);
            //confirming that there is one product in repo as it should be
            assertEquals(1, productRepository.findAll().size());

            mockMvc.perform(delete("/products/" + product.getId())
                            .with(user(admin.getUsername()).password(admin.getPassword())
                                    .authorities(utilPopulate.getAuthorities(admin))))
                    .andExpect(redirectedUrl("/products"));
            //product should be vanished
            assertEquals(0, productRepository.findAll().size());
        }

        @Test
        public void whenDeleteMany_thenProductsDeletedFromRepository() throws Exception {
            //creating shop owner and products for the shop
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            Shop shop = utilPopulate.createShopForAdmin(admin);
            Product product1 = utilPopulate.createProductForShop(shop);
            Product product2 = utilPopulate.createProductForShop(shop);
            //confirming that there are two products in repo as it should be
            assertEquals(2, productRepository.findAll().size());

            mockMvc.perform(post("/products/deleteMany")
                            .with(user(admin.getUsername()).password(admin.getPassword())
                                    .authorities(utilPopulate.getAuthorities(admin)))
                            .param("prods", product1.getId() + "", product2.getId() + ""))
                    .andExpect(redirectedUrl("/products"));
            //products should be vanished
            assertEquals(0, productRepository.findAll().size());
        }

        @Test
        public void testCacheMethodsFromProductService() {
            //creating shop with owner/admin and product
            Account admin = utilPopulate.createUser(Authority.ADMIN);
            Shop shop = utilPopulate.createShopForAdmin(admin);
            Product product = utilPopulate.createProductForShop(shop);
            Long id = product.getId();
            //invoking cacheable method
            productService.getProduct(id, admin.getUsername());
            //check if this product is added into the cache
            assertEquals(product, getCachedProduct(id));
            //updating product in repository
            product.setBalance(123.45);
            productService.saveProduct(product);
            //check if updated product is put into the cache
            assertEquals(product, getCachedProduct(id));
            assertEquals(123.45, getCachedProduct(id).getBalance(), 0.0);
            //invoking cache evict method
            productService.deleteProduct(id, admin.getUsername());
            //check if this product is deleted from cache
            assertNull(getCachedProduct(id));
        }

    private Product getCachedProduct(Long id) {
        return cacheManager.getCache("products").get(id, Product.class);
    }

}
