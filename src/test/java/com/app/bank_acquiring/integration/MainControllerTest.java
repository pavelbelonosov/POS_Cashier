package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.service.ShopService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MainControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopService shopService;
    @Autowired
    private UtilPopulate utilPopulate;

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
    public void givenHeadCashierUserWithWorkingTerminalTid_whenGetCashierView_thenStatusIsOkAndModelHasAdminWithTerminal() throws Exception {
        //creating admin user with shop and terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal1 = utilPopulate.createTerminalForShop(shop, admin);
        Terminal terminal2 = utilPopulate.createTerminalForShop(shop, admin);
        //creating employee and setting 1-st terminal as working for the account
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        employee.setWorkTerminalTid(terminal1.getTid());
        accountRepository.save(employee);

        MvcResult res = mockMvc.perform(get("/main")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andReturn();
        //Model should contain current account and terminal which is set as working
        Account accInModel = (Account) res.getModelAndView().getModel().get("account");
        assertTrue(accInModel.getId().equals(employee.getId()));

        Terminal terminalInModel = (Terminal) res.getModelAndView().getModel().get("terminal");
        assertTrue(terminalInModel.getId().equals(terminal1.getId()));
    }

    @Test
    public void givenAdminUserWithWorkingTerminalTid_whenGetCashierView_thenStatusIsOkAndModelHasAdminWithTerminal() throws Exception {
        //creating admin user with shop and terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal1 = utilPopulate.createTerminalForShop(shop, admin);
        Terminal terminal2 = utilPopulate.createTerminalForShop(shop, admin);
        //setting created terminal as working for admin
        admin.setWorkTerminalTid(terminal1.getTid());
        accountRepository.save(admin);

        MvcResult res = mockMvc.perform(get("/main")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andReturn();

        Account accInModel = (Account) res.getModelAndView().getModel().get("account");
        assertTrue(accInModel.getId().equals(admin.getId()));

        Terminal terminalInModel = (Terminal) res.getModelAndView().getModel().get("terminal");
        assertTrue(terminalInModel.getId().equals(terminal1.getId()));
    }

    @Test
    public void givenAdminUserWithoutWorkingTerminalTid_whenGetCashierView_thenStatusIsOkAndModelHasAdminWithTerminal() throws Exception {
        //creating admin user only with shop
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);

        MvcResult res = mockMvc.perform(get("/main")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andReturn();

        Account accInModel = (Account) res.getModelAndView().getModel().get("account");
        assertTrue(accInModel.getId().equals(admin.getId()));
        //when working terminal not set there is no terminal in model
        Terminal terminalInModel = (Terminal) res.getModelAndView().getModel().get("terminal");
        assertNull(terminalInModel);
    }


}
