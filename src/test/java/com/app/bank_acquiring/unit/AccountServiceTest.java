package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import org.junit.*;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@ActiveProfiles("test")
public class AccountServiceTest {

    private String password = "password";

    private AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
    private AccountInfoRepository accountInfoRepository = Mockito.mock(AccountInfoRepository.class);
    private ShopRepository shopRepository = Mockito.mock(ShopRepository.class);
    private PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

    private AccountService accountService;

    @Before
    public void setUp() {
        Mockito.when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPwd");
        Mockito.when(passwordEncoder.matches(any(String.class), eq("encodedPwd"))).thenReturn(true);
        accountService = new AccountService(accountRepository, passwordEncoder,
                accountInfoRepository, shopRepository);
    }

    @Test
    public void whenCreateAdmin_thenCreatesUserWithAdminAuthority_andEncodesPwdInDB() {
        Account admin = createMockedUserInRepository();
        accountService.createAdminUser(admin, new AccountInfo());
        Account acc = accountRepository.findByUsername(admin.getUsername());
        assertTrue(acc.getAuthority() == Authority.ADMIN);
        assertFalse(acc.getPassword().equals(password));
    }

    @Test
    public void whenCreateEmployee_thenCreatesUserWithoutAdminAuthority_andEncodesPwdInDB() {
        Account employee = createMockedUserInRepository();
        accountService.createEmployee(employee, new AccountInfo(), createMockedShopInRepository("shop"));
        Account acc = accountRepository.findByUsername(employee.getUsername());
        assertFalse(acc.getAuthority() == Authority.ADMIN);
        assertFalse(acc.getPassword().equals(password));
    }


    @Test
    public void whenUpdateEmployeeAccount_thenChangesAccountInfo() {
        Account employee = createMockedUserInRepository();
        AccountInfo newEmployeeInfo = createAccountInfoWithData();
        Authority newEmployeeAuthority = Authority.CASHIER;
        Shop shop = createMockedShopInRepository("shop");
        mockUserWithShop(employee, shop);

        accountService.createEmployee(employee, new AccountInfo(), shop);
        accountService.updateEmployeeAccount(employee.getId(), newEmployeeInfo,
                createMockedShopInRepository("otherShop"), newEmployeeAuthority);
        Account acc = accountRepository.findByUsername(employee.getUsername());

        assertTrue(acc.getAccountInfo().getEmail().equals(newEmployeeInfo.getEmail()));
        assertTrue(acc.getAccountInfo().getTelephoneNumber().equals(newEmployeeInfo.getTelephoneNumber()));
        assertTrue(acc.getAccountInfo().getLastName().equals(newEmployeeInfo.getLastName()));
        assertTrue(acc.getAccountInfo().getFirstName().equals(newEmployeeInfo.getFirstName()));
        assertTrue(acc.getAuthority() == newEmployeeAuthority);
    }

    @Test
    public void whenUpdateCurrentAccount_thenChangesAccountInfo() {
        Account current = createMockedUserInRepository();
        AccountInfo newAccInfo = createAccountInfoWithData();
        accountService.createAdminUser(current, new AccountInfo());
        Account acc = accountRepository.findByUsername(current.getUsername());
        accountService.updateCurrentAccount(acc, newAccInfo);

        assertTrue(acc.getAccountInfo().getEmail().equals(newAccInfo.getEmail()));
        assertTrue(acc.getAccountInfo().getTelephoneNumber().equals(newAccInfo.getTelephoneNumber()));
        assertTrue(acc.getAccountInfo().getLastName().equals(newAccInfo.getLastName()));
        assertTrue(acc.getAccountInfo().getFirstName().equals(newAccInfo.getFirstName()));
    }

    @Test
    public void whenChangeCurrentAccountPassword_thenPwdChanged() {
        Account current = createMockedUserInRepository();
        String newPassword = "newPassword";
        accountService.changeCurrentAccountPassword(current, newPassword);
        Account acc = accountRepository.findByUsername(current.getUsername());
        assertTrue(passwordEncoder.matches(newPassword, acc.getPassword()));
    }

    @Test
    public void whenChangeEmployeePassword_thenPwdChanged() {
        Account employee = createMockedUserInRepository();
        String newPassword = "newPassword";
        accountService.createEmployee(employee, new AccountInfo(), createMockedShopInRepository("shop"));
        accountService.changeEmployeePassword(employee.getId(), newPassword);
        Account acc = accountRepository.findByUsername(employee.getUsername());
        assertTrue(passwordEncoder.matches(newPassword, acc.getPassword()));
    }

    private Account createMockedUserInRepository() {
        Account user = Mockito.spy(Account.class);
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(password);
        user.setAccountInfo(new AccountInfo());
        user.setId(Math.abs(new Random().nextLong()));

        Mockito.when(accountRepository.findByUsername(user.getUsername()))
                .thenReturn(user);
        Mockito.when(accountRepository.getOne(user.getId()))
                .thenReturn(user);
        //Mockito.when(accountRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    private Shop createMockedShopInRepository(String shopName) {
        Shop shop = new Shop();
        shop.setName(shopName);
        List<Account> accountList = new ArrayList<>();
        shop.setAccounts(accountList);
        shop.setId(Math.abs(new Random().nextLong()));
        Mockito.when(shopRepository.getOne(shop.getId()))
                .thenReturn(shop);
        return shop;
    }

    private AccountInfo createAccountInfoWithData() {
        return new AccountInfo("John", "Doe", "email@mail.com",
                "9012345678", null);
    }

    //to mock @ManyToMany relation between Accounts and Shops
    private void mockUserWithShop(Account account, Shop shop) {
        Mockito.when(account.getShops()).thenReturn(Arrays.asList(new Shop[]{shop}));
    }

}
