package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.ShopService;
import com.app.bank_acquiring.service.UposService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
public class ShopServiceTest {

    private ShopService shopService;
    private ShopRepository shopRepository = Mockito.mock(ShopRepository.class);
    private AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
    private AccountInfoRepository accountInfoRepository = Mockito.mock(AccountInfoRepository.class);
    private UposService uposService = Mockito.mock(UposService.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        shopService = new ShopService(accountRepository, accountInfoRepository, shopRepository, uposService);
    }

    @Test
    public void whenBundleShopWithAccount_thenAddsAccountToShopAndSavesShopInRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        assertTrue(shop.getAccounts() == null);
        shopService.bundleShopWithAccount(shop, account.getUsername());
        Mockito.verify(shopRepository, times(1)).save(shop);
        assertTrue(shop.getAccounts().contains(account));

    }

    @Test
    public void givenCorrectShopIdAndAccount_whenGetShop_thenReturnsShopFromRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.bundleShopWithAccount(shop, account.getUsername());
        mockUserWithShop(account, shop);
        assertNotNull(shopService.getShop(shop.getId(), account.getUsername()));
    }

    @Test
    public void givenIncorrectShopIdAndAccount_whenGetShop_thenThrowsRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current account doesn't have access to this shop");

        Account account1 = createUserWithAccountInfo();
        Shop shop1 = createShop();
        shopService.bundleShopWithAccount(shop1, account1.getUsername());
        mockUserWithShop(account1, shop1);
        Account account2 = createUserWithAccountInfo();
        Shop shop2 = createShop();
        shopService.bundleShopWithAccount(shop2, account2.getUsername());
        mockUserWithShop(account2, shop2);
        //must throw RuntimeException because of ids' validation
        shopService.getShop(shop1.getId(), account2.getUsername());
    }


    @Test
    public void givenCurrentAccountId_whenDeleteAccountFromShop_thenDeletesAccountFromRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.bundleShopWithAccount(shop, account.getUsername());
        mockUserWithShop(account, shop);
        assertTrue(account.getId() > 0);
        shopService.deleteAccountFromShop(shop.getId(), account.getId(), account.getUsername());
        assertTrue(account.getId() < 0);
    }

    @Test
    public void givenEmployeeId_whenDeleteAccountFromShop_thenDeletesEmployeeFromRepository() {
        Account shopOwner = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.bundleShopWithAccount(shop, shopOwner.getUsername());
        mockUserWithShop(shopOwner, shop);

        Account shopEmployee = createUserWithAccountInfo();
        shopService.bundleShopWithAccount(shop, shopEmployee.getUsername());
        mockUserWithShop(shopEmployee, shop);

        assertTrue(shopEmployee.getId() > 0);
        shopService.deleteAccountFromShop(shop.getId(), shopEmployee.getId(), shopOwner.getUsername());
        assertTrue(shopEmployee.getId() == -1L);
    }


    @Test
    public void givenIncorrectAccId_whenDeleteAccountFromShop_thenThrowsRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current shop doesn't have access to this employee");

        Account account1 = createUserWithAccountInfo();
        Shop shop1 = createShop();
        shopService.bundleShopWithAccount(shop1, account1.getUsername());
        mockUserWithShop(account1, shop1);
        Account account2 = createUserWithAccountInfo();
        Shop shop2 = createShop();
        shopService.bundleShopWithAccount(shop2, account2.getUsername());
        mockUserWithShop(account2, shop2);
        //must throw RuntimeException because of ids' validation
        shopService.deleteAccountFromShop(shop1.getId(), account2.getId(), account1.getUsername());
    }

    @Test
    public void givenIncorrectShopId_whenDeleteAccountFromShop_thenThrowsRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current account doesn't have access to this shop");

        Account account1 = createUserWithAccountInfo();
        Shop shop1 = createShop();
        shopService.bundleShopWithAccount(shop1, account1.getUsername());
        mockUserWithShop(account1, shop1);
        Account account2 = createUserWithAccountInfo();
        Shop shop2 = createShop();
        shopService.bundleShopWithAccount(shop2, account2.getUsername());
        mockUserWithShop(account2, shop2);
        //must throw RuntimeException because of ids' validation
        shopService.deleteAccountFromShop(shop2.getId(), account2.getId(), account1.getUsername());
    }

    @Test
    public void whenDeleteShop_thenDeletesShopWithEmployeesFromRepository() {
        Account owner = createUserWithAccountInfo();
        Shop shop = createShop();
        Mockito.when(shop.getTerminals()).thenReturn(new ArrayList<>());
        shopService.bundleShopWithAccount(shop, owner.getUsername());
        mockUserWithShop(owner, shop);

        Account employee = createUserWithAccountInfo();
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        mockUserWithShop(employee, shop);

        assertTrue(owner.getId() > 0);
        assertTrue(employee.getId() > 0);
        assertTrue(shop.getId() > 0);
        shopService.deleteShop(shop.getId(), owner.getUsername());
        assertTrue(owner.getId() > 0);
        assertTrue(employee.getId() == -1L);
        assertTrue(shop.getId() == -1L);
    }


    @Test
    public void givenIncorrectShopId_whenDeleteShop_thenThrowsRuntimeException() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Current account doesn't have access to this shop");

        Account owner1 = createUserWithAccountInfo();
        Shop shop1 = createShop();
        shopService.bundleShopWithAccount(shop1, owner1.getUsername());
        mockUserWithShop(owner1, shop1);
        Account owner2 = createUserWithAccountInfo();
        Shop shop2 = createShop();
        shopService.bundleShopWithAccount(shop2, owner2.getUsername());
        mockUserWithShop(owner2, shop2);
        //must throw RuntimeException because of ids' validation
        shopService.deleteShop(shop2.getId(), owner1.getUsername());
    }

    private Account createUserWithAccountInfo() {
        Account user = spy(Account.class);
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setId(Math.abs(new Random().nextLong()));
        user.setAccountInfo(new AccountInfo());

        Mockito.when(accountRepository.findByUsername(user.getUsername())).thenReturn(user);
        Mockito.when(accountRepository.findById(user.getId())).thenReturn(Optional.of(user));
        doAnswer(invocationOnMock -> {
            Account arg = invocationOnMock.getArgument(0);
            arg.setId(-1L);
            return null;
        }).when(accountRepository).delete(any(Account.class));
        return user;
    }

    private Shop createShop() {
        Shop shop = spy(Shop.class);
        shop.setName("shop");
        shop.setId(Math.abs(new Random().nextLong()));
        Mockito.when(shopRepository.findById(shop.getId())).thenReturn(Optional.of(shop));
        doAnswer(invocationOnMock -> {
            Shop arg = invocationOnMock.getArgument(0);
            arg.setId(-1L);
            return null;
        }).when(shopRepository).delete(any(Shop.class));
        return shop;
    }

    //to mock @ManyToMany relation between Accounts and Shops
    private void mockUserWithShop(Account account, Shop shop) {
        Mockito.when(account.getShops()).thenReturn(List.of(shop));
    }


}
