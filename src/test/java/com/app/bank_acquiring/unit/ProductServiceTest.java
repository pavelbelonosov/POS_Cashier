package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.ProductService;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionSystemException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopRepository shopRepository;

    @After
    public void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    public void whenGetProduct_thenReturnValidatedProduct() {
        Account account = createUser();
        Product product = createProductForAccount(account);
        assertNotNull(productService.getProduct(product.getId(), account.getUsername()));
    }

    @Test(expected = RuntimeException.class)
    public void givenWrongAccount_whenGetProduct_thenThrownRuntimeException() {
        Account account = createUser();
        Product product = createProductForAccount(account);

        Account accountWithNoProducts = createUser();
        productService.getProduct(product.getId(), accountWithNoProducts.getUsername());
    }

    @Test
    public void whenDeleteProduct_thenProductIsDeleted() {
        Account account = createUser();
        Product product = createProductForAccount(account);
        assertNotNull(productRepository.getOne(product.getId()));
        productService.deleteProduct(product.getId(), account.getUsername());
        assertTrue(productRepository.findById(product.getId()).isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void givenWrongAccount_whenDeleteProduct_thenThrownRuntimeException() {
        Account account = createUser();
        Product product = createProductForAccount(account);

        Account accountWithNoProducts = createUser();
        productService.deleteProduct(product.getId(), accountWithNoProducts.getUsername());
    }

    @Test
    public void whenCopyProducts_thenProductsAreCopiedFromOneShopToOther() {
        Account account = createUser();
        Product product = createProductForAccount(account);
        Shop targetShop = createShopForAccount("targetShop", account);
        productService.copyProducts(new long[]{product.getId()}, product.getShop().getId(),
                targetShop.getId(), account.getUsername());
        List<Product> products = productRepository.findAll();
        assertTrue(products.size() == 2
                && products.get(1).getShop().getName().equals(targetShop.getName()));
    }

    @Test(expected = RuntimeException.class)
    public void givenWrongShop_whenCopyProducts_thenThrownRuntimeException() {
        Account account = createUser();
        Product product = createProductForAccount(account);
        Shop targetShop = createShopForAccount("targetShop", createUser());
        productService.copyProducts(new long[]{product.getId()}, product.getShop().getId(),
                targetShop.getId(), account.getUsername());
    }

    @Test
    public void whenCreateExcelFile_thenFileCreated() {
        Account account = createUser();
        Product product = createProductForAccount(account);
        byte[] byteArr = productService.createExcelFile(account.getId(), product.getShop().getId(),
                Arrays.asList(new Product[]{product}));
        assertTrue(byteArr.length > 0);
    }

    @Test
    public void whenSaveProduct_thenProductIsPersistedInRepository() {
        Product product = new Product();
        product.setName("product");
        productService.saveProduct(product);
        assertNotNull(productRepository.getOne(product.getId()));
    }

    @Test(expected = TransactionSystemException.class)
    public void givenProductWithEmptyName_whenSaveProduct_thenTransactionExceptionOfValidationError() {
        Product product = new Product();
        product.setName("");
        productService.saveProduct(product);
    }

    private Account createUser() {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        return accountRepository.save(user);
    }

    private Product createProductForAccount(Account account) {
        Product product = new Product();
        product.setShop(createShopForAccount("shop", account));
        product.setName("product");
        return productRepository.save(product);
    }

    private Shop createShopForAccount(String shopName, Account account) {
        Shop shop = new Shop();
        shop.setName(shopName);
        List<Account> accountList = new ArrayList<>();
        accountList.add(account);
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }
}
