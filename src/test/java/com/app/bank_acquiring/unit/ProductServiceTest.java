package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.ShopService;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.junit.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class ProductServiceTest {

    private ProductService productService;
    private ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
    private ShopService shopService = Mockito.mock(ShopService.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        productService = new ProductService(productRepository, accountRepository, shopService);
    }

    @Test
    public void whenGetProduct_thenReturnProductBelongToAccount() {
        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        assertNotNull(productService.getProduct(product.getId(), account.getUsername()));
    }

    @Test
    public void givenWrongAccount_whenGetProduct_thenThrownRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current user doesn't have access to this product");

        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        Account accountWithNoProducts = createMockedUserInRepository();
        productService.getProduct(product.getId(), accountWithNoProducts.getUsername());
    }

    @Test
    public void whenDeleteProduct_thenProductIsDeleted() {
        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        productService.deleteProduct(product.getId(), account.getUsername());
        assertNull(productService.getProduct(product.getId(), account.getUsername()));
    }

    @Test
    public void givenWrongAccount_whenDeleteProduct_thenThrownRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current user doesn't have access to this product");

        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        Account accountWithoutProducts = createMockedUserInRepository();
        productService.deleteProduct(product.getId(), accountWithoutProducts.getUsername());
    }


    @Test
    public void whenCopyProducts_thenProductFieldsAreCopiedIntoNewProductAndInvokesSavingInRepository() {
        Account account = createMockedUserInRepository();
        Shop targetShop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(targetShop);
        mockUserWithShop(account, targetShop);

        ArgumentCaptor<Product> valueCapture = ArgumentCaptor.forClass(Product.class);
        productService.copyProducts(new long[]{product.getId()}, product.getShop().getId(),
                targetShop.getId(), account.getUsername());

        Mockito.verify(productRepository, times(1)).save(valueCapture.capture());
        assertTrue(valueCapture.getValue().getId() == null);
        assertTrue(valueCapture.getValue().getName().equals(product.getName()));
    }

    @Test
    public void givenWrongShop_whenCopyProducts_thenThrownRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current account doesn't have access to this shop");

        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        Long wrongShopId = shop.getId() + 1;
        productService.copyProducts(new long[]{product.getId()}, product.getShop().getId(),
                wrongShopId, account.getUsername());
    }


    @Test
    public void whenCreateExcelFile_thenFileCreated() {
        Account account = createMockedUserInRepository();
        Shop shop = createMockedShopForAccountInRepository(account);
        Product product = createMockedProductForShopInRepository(shop);
        mockUserWithShop(account, shop);

        byte[] byteArr = productService.createExcelFile(account.getId(), product.getShop().getId(),
                Arrays.asList(new Product[]{product}));
        assertTrue(byteArr.length > 0);
    }


    @Test
    public void whenSaveProduct_thenInvokeSavingInRepository() {
        Product product = new Product();
        product.setName("product");

        ArgumentCaptor<Product> valueCapture = ArgumentCaptor.forClass(Product.class);
        productService.saveProduct(product);

        Mockito.verify(productRepository, times(1)).save(valueCapture.capture());
        assertTrue(valueCapture.getValue().getName().equals(product.getName()));
    }

    @Test
    public void whenSaveNullProduct_thenNotInvokeSavingInRepository() {
        productService.saveProduct(null);
        Mockito.verify(productRepository, times(0)).save(any(Product.class));

    }

    private Account createMockedUserInRepository() {
        Account user = Mockito.spy(Account.class);
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setId(new Random().nextLong());

        Mockito.when(accountRepository.save(user)).thenReturn(user);
        Mockito.when(accountRepository.findByUsername(user.getUsername())).thenReturn(user);
        return accountRepository.save(user);
    }

    private Product createMockedProductForShopInRepository(Shop shop) {
        Product product = new Product();
        product.setShop(shop);
        product.setName("product");
        product.setId(Math.abs(new Random().nextLong()));

        Mockito.when(productRepository.getOne(product.getId())).thenReturn(product);
        Mockito.when(productRepository.getOne(-1L)).thenReturn(null);
        doAnswer(invocationOnMock -> {
            product.setId(-1L);
            return null;
        }).when(productRepository).delete(product);

        return product;
    }

    private Shop createMockedShopForAccountInRepository(Account account) {
        Shop shop = new Shop();
        shop.setId(new Random().nextLong());
        shop.setName("shopName");
        shop.setAccounts(List.of(account));

        Mockito.when(shopService.getShop(shop.getId(), account.getUsername())).thenReturn(shop);
        Mockito.when(shopService.getShop(AdditionalMatchers.not(eq(shop.getId())), anyString())).
                thenThrow(new RuntimeException("Current account doesn't have access to this shop"));

        return shop;
    }

    private void mockUserWithShop(Account account, Shop shop) {
        Mockito.when(account.getShops()).thenReturn(List.of(shop));
    }
}
