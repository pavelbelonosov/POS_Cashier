package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.ShopService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;

import static org.junit.Assert.*;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ShopServiceTest {

    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopService shopService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;

    @After
    public void tearDown() {
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
    }

    @Test
    public void whenCreateShop_thenSavesShopInRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());
        assertNotNull(shopRepository.getOne(shop.getId()));
    }

    @Test
    public void givenCorrectShopIdAndAccount_whenGetShop_thenReturnsShopFromRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());
        assertNotNull(shopService.getShop(shop.getId(), account.getUsername()));
    }

    @Test(expected = RuntimeException.class)
    public void givenIncorrectShopIdAndAccount_whenGetShop_thenThrowsRuntimeException() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());
        Shop wrongShop = createShop();
        shopService.getShop(wrongShop.getId(), account.getUsername());
    }

    @Test
    public void whenDeleteAccountFromShop_thenDeletesAccountFromRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());
        Long id = account.getId();
        shopService.deleteAccountFromShop(shop.getId(), account.getId(), account.getUsername());
        assertTrue(accountRepository.findById(id).isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void givenIncorrectShopId_whenDeleteAccountFromShop_thenThrowsRuntimeException() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());

        Shop wrongShop = createShop();
        shopService.deleteAccountFromShop(wrongShop.getId(), account.getId(), account.getUsername());
    }

    @Test
    public void whenDeleteShop_thenDeletesShopFromRepository() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());
        Long id = account.getId();
        shopService.deleteShop(shop.getId(), account.getUsername());
        assertTrue(shopRepository.findById(id).isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void givenIncorrectShopId_whenDeleteShop_thenThrowsRuntimeException() {
        Account account = createUserWithAccountInfo();
        Shop shop = createShop();
        shopService.createShop(shop, account.getUsername());

        Shop wrongShop = createShop();
        shopService.deleteShop(wrongShop.getId(), account.getUsername());
    }

    @Test
    public void equality(){
        Account account1 = createUserWithAccountInfo();
        System.out.println(account1.getId());
        Account account2 = accountRepository.findByUsername(account1.getUsername());
        System.out.println(account2.getId());
        assertTrue(account1.equals(account2));

        Account user1 = new Account();
        user1.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user1.setPassword("password");

        Account user2  = new Account();
        user2 .setUsername(user1.getUsername());
        user2 .setPassword("password");

        assertTrue(user1.equals(user2));
    }

    private Account createUserWithAccountInfo() {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccount(user);
        accountInfoRepository.save(accountInfo);
        return accountRepository.save(user);
    }

    private Shop createShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        return shop;
    }


}
