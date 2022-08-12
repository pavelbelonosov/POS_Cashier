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
import com.app.bank_acquiring.service.IdValidationException;
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

import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        Terminal terminal = createTerminalForShopInRepository(shop, admin);
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

    @Test
    public void givenAdminAccount_whenGetTerminalById_thenStatusOk() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);
        MvcResult res = mockMvc.perform(get("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("terminal"))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(terminal.getTid()));
    }

    @Test
    public void givenWrongIds_whenGetTerminalById_thenThrowsException() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin1 = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin1);
        Terminal terminal = createTerminalForShopInRepository(shop, admin1);
        //when sending wrong existing user -> should throw Exception due to validation issue
        Account admin2 = createUserInRepository(Authority.ADMIN);
        mockMvc.perform(get("/terminals/" + terminal.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"));

        //when sending not existing terminal -> should throw Exception due to validation issue
        mockMvc.perform(get("/terminals/" + 500)
                        .with(user(admin1.getUsername()).password(admin1.getPassword())
                                .authorities(getAuthorities(admin1))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"));
    }

    @Test
    public void givenAdminAccount_whenTestTerminalConnection_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);

        mockMvc.perform(get("/terminals/" + terminal.getId() + "/test")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
    }

    @Test
    public void givenAdminAccount_whenSetTerminalToCurrentAccount_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating detached terminal with shop
        Terminal terminal = createDetachedTerminal();
        terminal.setShop(shop);

        mockMvc.perform(post("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("terminal", terminal))
                .andExpect(redirectedUrl("/terminals"));
        // terminal should be saved in repo
        assertNotNull(terminalRepository.findByTid(terminal.getTid()));
    }

    @Test
    public void givenNotValidFields_whenSetTerminalToCurrentAccount_thenReturnBindingErrors() throws Exception {
        //creating shop with owner/admin
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        //creating detached terminal with shop
        Terminal terminal = createDetachedTerminal();
        terminal.setTid("");//not valid constraint
        terminal.setIp("");//not valid constraint

        mockMvc.perform(post("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .flashAttr("terminal", terminal))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("terminal", "shop", "tid", "ip"))
                .andExpect(view().name("terminals"));
        // terminal should not be saved in repo
        assertNull(terminalRepository.findByTid(terminal.getTid()));
    }

    @Test
    public void givenAdminAccount_whenSetWorkTerminalToAccount_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);

        mockMvc.perform(post("/accounts/current/terminals/workingterminal")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .param("terminalId", terminal.getId() + ""))
                .andExpect(redirectedUrl("/main"));
        // terminal's tid should be added to account's workTerminal field
        assertTrue(accountRepository.findByUsername(admin.getUsername())
                .getWorkTerminalTid().equals(terminal.getTid()));
    }

    @Test
    public void givenIkr_whenUpdateTerminal_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);
        String newIp = "99.99.99.99";
        String newHeader = "newHeader";

        mockMvc.perform(post("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .param("connection", false + "")//integrated POS type
                        .param("ip", newIp)
                        .param("chequeHeader", newHeader))
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
        // terminal's fields should be updated in repo
        Terminal updatedTerm = terminalRepository.findByTid(terminal.getTid());
        assertTrue(updatedTerm.getIp().equals(newIp));
        assertTrue(updatedTerm.getChequeHeader().equals(newHeader));
    }

    @Test
    public void givenIkrAndNotValidParams_whenUpdateTerminal_thenTerminalNotUpdatedInDB() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);
        String currentIp = terminal.getIp().intern();
        String currentHeader = terminal.getChequeHeader();
        String newIp = "  ";//not valid
        String newHeader = ""; //not valid

        mockMvc.perform(post("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        .param("connection", false + "")//integrated POS
                        .param("ip", newIp)
                        .param("chequeHeader", newHeader))
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
        // terminal's fields should not be updated in repo
        Terminal updatedTerm = terminalRepository.findByTid(terminal.getTid());
        assertTrue(updatedTerm.getIp().equals(currentIp));
        assertTrue(updatedTerm.getChequeHeader().equals(currentHeader));
    }

    @Test
    public void givenAdmin_whenDeleteTerminal_thenTerminalIsDeletedFromRepository() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Terminal terminal = createTerminalForShopInRepository(shop, admin);

        mockMvc.perform(delete("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(redirectedUrl("/terminals"));
        //terminal should be deleted from repo
        assertNull(terminalRepository.findByTid(terminal.getTid()));
    }

    @Test
    public void givenWrongId_whenDeleteTerminal_thenStatusForbidden() throws Exception {
        //creating two shop two with owner/admin and two terminal in repos
        Account admin1 = createUserInRepository(Authority.ADMIN);
        Shop shop1 = createShopForAdminInRepository(admin1);
        Terminal terminal1 = createTerminalForShopInRepository(shop1, admin1);

        Account admin2 = createUserInRepository(Authority.ADMIN);
        Shop shop2 = createShopForAdminInRepository(admin2);
        Terminal terminal2 = createTerminalForShopInRepository(shop2, admin2);

        //sending terminal not belonging to authenticated admin
        mockMvc.perform(delete("/terminals/" + terminal2.getId())
                        .with(user(admin1.getUsername()).password(admin1.getPassword())
                                .authorities(getAuthorities(admin1))))
                //@ExceptionHandler catches IdValidationException
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                //response with error.html
                .andExpect(view().name("error"));
        //terminal should not be deleted from repo
        assertTrue(terminalRepository.findAll().size() == 2);
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

    private Terminal createTerminalForShopInRepository(Shop shop, Account account) {
        Terminal terminal = new Terminal();
        terminal.setTid((new Random().nextInt(1000)+10000000)+"");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        terminal.setStandalone(false);//integrated pos
        terminal.setShop(shop);
        terminal.setAccount(account);
        return terminalRepository.save(terminal);
    }


    private Terminal createDetachedTerminal() {
        Terminal terminal = new Terminal();
        terminal.setStandalone(false);
        terminal.setTid((new Random().nextInt(1000)+10000000)+"");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        return terminal;
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }
}
