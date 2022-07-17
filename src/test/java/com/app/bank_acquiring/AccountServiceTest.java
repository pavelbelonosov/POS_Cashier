package com.app.bank_acquiring;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountServiceTest {

    private String password = "password";

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Before
    public void setUp() {

    }

    @Test
    public void whenCreateAdmin_thenCreatesUserWithAdminAuthority_andEncodesPwdInDB() {
        Account admin = createUser();
        accountService.createAdminUser(admin, new AccountInfo());
        Account acc = accountRepository.findByUsername(admin.getUsername());
        assertTrue(acc.getAuthority() == Authority.ADMIN);
        assertFalse(acc.getPassword().equals(password));
    }

    @Test(expected = TransactionSystemException.class)
    public void givenAccountWithEmptyNameAndPwd_whenCreateAdmin_thenJpaTransactionRollbackOfValidationError(){
        Account admin = new Account();
        admin.setUsername("");
        admin.setPassword("");
        accountService.createAdminUser(admin, new AccountInfo());
    }

    @Test
    public void whenCreateEmployee_thenCreatesUserWithoutAdminAuthority_andEncodesPwdInDB() {
        Account employee = createUser();
        accountService.createEmployee(employee, new AccountInfo(), createShop("shop"));
        Account acc = accountRepository.findByUsername(employee.getUsername());
        assertFalse(acc.getAuthority() == Authority.ADMIN);
        assertFalse(acc.getPassword().equals(password));
    }


    @Test
    public void whenUpdateEmployeeAccount_thenChangesAccountInfo() {
        Account employee = createUser();
        AccountInfo newEmployeeInfo = createAccountInfoWithData();
        Authority newEmployeeAuthority = Authority.CASHIER;
        accountService.createEmployee(employee, new AccountInfo(), createShop("shop"));
        accountService.updateEmployeeAccount(employee.getId(), newEmployeeInfo,
                createShop("otherShop"), newEmployeeAuthority);
        Account acc = accountRepository.findByUsername(employee.getUsername());
        assertTrue(acc.getAccountInfo().getEmail().equals(newEmployeeInfo.getEmail())
                && acc.getAccountInfo().getTelephoneNumber().equals(newEmployeeInfo.getTelephoneNumber())
                && acc.getAccountInfo().getLastName().equals(newEmployeeInfo.getLastName())
                && acc.getAccountInfo().getFirstName().equals(newEmployeeInfo.getFirstName()));
        assertTrue(acc.getAuthority() == newEmployeeAuthority);
    }

    @Test
    public void whenUpdateCurrentAccount_thenChangesAccountInfo() {
        Account current = createUser();
        AccountInfo newAccInfo = createAccountInfoWithData();
        accountService.createAdminUser(current, new AccountInfo());
        Account acc = accountRepository.findByUsername(current.getUsername());
        accountService.updateCurrentAccount(acc, newAccInfo);
        assertTrue(acc.getAccountInfo().getEmail().equals(newAccInfo.getEmail())
                && acc.getAccountInfo().getTelephoneNumber().equals(newAccInfo.getTelephoneNumber())
                && acc.getAccountInfo().getLastName().equals(newAccInfo.getLastName())
                && acc.getAccountInfo().getFirstName().equals(newAccInfo.getFirstName()));
    }

    @Test
    public void whenChangeCurrentAccountPassword_thenPwdChanged(){
        Account current = createUser();
        String newPassword = "newPassword";
        accountService.changeCurrentAccountPassword(current, newPassword);
        Account acc = accountRepository.findByUsername(current.getUsername());
        assertTrue(passwordEncoder.matches(newPassword, acc.getPassword()));
    }

    @Test
    public void whenChangEmployeePassword_thenPwdChanged(){
        Account employee = createUser();
        String newPassword = "newPassword";
        accountService.createEmployee(employee,new AccountInfo(),createShop("shop"));
        accountService.changeEmployeePassword(employee.getId(), newPassword);
        Account acc = accountRepository.findByUsername(employee.getUsername());
        assertTrue(passwordEncoder.matches(newPassword, acc.getPassword()));
    }

    private Account createUser() {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(password);
        return user;
    }

    private AccountInfo createAccountInfoWithData() {
        return new AccountInfo("John", "Doe", "email@mail.com",
                "9012345678", null);
    }

    private Shop createShop(String shopName) {
        Shop shop = new Shop();
        shop.setName(shopName);
        List<Account> accountList = new ArrayList<>();
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }


}
