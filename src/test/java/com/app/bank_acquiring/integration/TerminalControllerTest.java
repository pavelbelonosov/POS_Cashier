package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.*;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.TerminalService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TerminalControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TerminalService terminalService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private ShopRepository shopRepository;

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
        terminalRepository.deleteAll();
    }

    @Test
    public void givenAdminAccount_whenGetTerminals_thenStatusOk() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop);
        MvcResult res = mockMvc.perform(get("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("terminals"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(terminal.getTid()));
    }

    @Test
    public void givenNonAdmin_whenGetTerminals_thenStatusIsForbidden() throws Exception {
        Account cashier = createUserInRepository(Authority.CASHIER);
        Account hCashier = createUserInRepository(Authority.HEAD_CASHIER);
        mockMvc.perform(get("/terminals")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(getAuthorities(cashier))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/terminals")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(getAuthorities(hCashier))))
                .andExpect(status().isForbidden());
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

    private Shop createShopForAdminInRepository(Account admin) {
        Shop shop = new Shop();
        shop.setName("shop");
        List<Account> accountList = new ArrayList<>();
        accountList.add(admin);
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }

    private Terminal createTerminalForShopInRepository(Shop shop) {
        Terminal terminal = new Terminal();
        terminal.setTid("12345678");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setShop(shop);
        return terminalRepository.save(terminal);
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }
}
