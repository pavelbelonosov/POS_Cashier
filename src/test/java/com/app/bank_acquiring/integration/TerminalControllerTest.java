package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.*;
import com.app.bank_acquiring.service.IdValidationException;
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
    private AccountRepository accountRepository;
    @Autowired
    private TerminalRepository terminalRepository;
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
    public void givenAdminAccount_whenGetTerminals_thenStatusOk() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);
        MvcResult res = mockMvc.perform(get("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("terminals"))
                .andReturn();
        //response should contain info about terminal
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(terminal.getTid()));
    }

    @Test
    public void givenNonAdmin_whenGetTerminals_thenStatusIsForbidden() throws Exception {
        Account cashier = utilPopulate.createUser(Authority.CASHIER);
        Account hCashier = utilPopulate.createUser(Authority.HEAD_CASHIER);
        mockMvc.perform(get("/terminals")
                        .with(user(cashier.getUsername()).password(cashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(cashier))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/terminals")
                        .with(user(hCashier.getUsername()).password(hCashier.getPassword())
                                .authorities(utilPopulate.getAuthorities(hCashier))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenAdminAccount_whenGetTerminalById_thenStatusOk() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);
        MvcResult res = mockMvc.perform(get("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("terminal"))
                .andReturn();
        //response should contain info about terminal
        String content = res.getResponse().getContentAsString();
        assertTrue(content.contains(terminal.getTid()));
    }

    @Test
    public void givenWrongIds_whenGetTerminalById_thenThrowsException() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin1 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin1);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin1);
        //when sending wrong existing user -> should throw Exception due to validation issue
        Account admin2 = utilPopulate.createUser(Authority.ADMIN);
        mockMvc.perform(get("/terminals/" + terminal.getId())
                        .with(user(admin2.getUsername()).password(admin2.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin2))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"));

        //when sending not existing terminal -> should throw Exception due to validation issue
        mockMvc.perform(get("/terminals/" + 500)
                        .with(user(admin1.getUsername()).password(admin1.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin1))))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"));
    }

    @Test
    public void givenAdminAccount_whenTestTerminalConnection_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin and terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);

        mockMvc.perform(get("/terminals/" + terminal.getId() + "/test")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
    }

    @Test
    public void givenAdminAccount_whenSetTerminalToCurrentAccount_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //creating detached terminal with shop
        Terminal terminal = utilPopulate.createDetachedTerminal();
        terminal.setShop(shop);

        mockMvc.perform(post("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .flashAttr("terminal", terminal))
                .andExpect(redirectedUrl("/terminals"));
        // terminal should be saved in repo
        assertNotNull(terminalRepository.findByTid(terminal.getTid()));
    }

    @Test
    public void givenNotValidFields_whenSetTerminalToCurrentAccount_thenReturnBindingErrors() throws Exception {
        //creating shop with owner/admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        //creating detached terminal with shop
        Terminal terminal = utilPopulate.createDetachedTerminal();
        terminal.setTid("");//not valid constraint
        terminal.setIp("");//not valid constraint

        mockMvc.perform(post("/terminals")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
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
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);

        mockMvc.perform(post("/accounts/current/terminals/workingterminal")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .param("terminalId", terminal.getId() + ""))
                .andExpect(redirectedUrl("/main"));
        // terminal's tid should be added to account's workTerminal field
        assertEquals(accountRepository.findByUsername(admin.getUsername())
                .getWorkTerminalTid(), terminal.getTid());
    }

    @Test
    public void givenIkr_whenUpdateTerminal_thenStatusRedirect() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);
        String newIp = "99.99.99.99";
        String newHeader = "newHeader";

        mockMvc.perform(post("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .param("connection", false + "")//integrated POS type
                        .param("ip", newIp)
                        .param("chequeHeader", newHeader))
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
        // terminal's fields should be updated in repo
        Terminal updatedTerm = terminalRepository.findByTid(terminal.getTid());
        assertEquals(updatedTerm.getIp(), newIp);
        assertEquals(updatedTerm.getChequeHeader(), newHeader);
    }

    @Test
    public void givenIkrAndNotValidParams_whenUpdateTerminal_thenTerminalNotUpdatedInDB() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);
        String currentIp = terminal.getIp().intern();
        String currentHeader = terminal.getChequeHeader();
        String newIp = "  ";//not valid
        String newHeader = ""; //not valid

        mockMvc.perform(post("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        .param("connection", false + "")//integrated POS
                        .param("ip", newIp)
                        .param("chequeHeader", newHeader))
                .andExpect(redirectedUrl("/terminals/" + terminal.getId()));
        // terminal's fields should not be updated in repo
        Terminal updatedTerm = terminalRepository.findByTid(terminal.getTid());
        assertEquals(updatedTerm.getIp(), currentIp);
        assertEquals(updatedTerm.getChequeHeader(), currentHeader);
    }

    @Test
    public void givenAdmin_whenDeleteTerminal_thenTerminalIsDeletedFromRepository() throws Exception {
        //creating shop with owner/admin and terminal in repos
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Terminal terminal = utilPopulate.createTerminalForShop(shop, admin);

        mockMvc.perform(delete("/terminals/" + terminal.getId())
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(redirectedUrl("/terminals"));
        //terminal should be deleted from repo
        assertNull(terminalRepository.findByTid(terminal.getTid()));
    }

    @Test
    public void givenWrongId_whenDeleteTerminal_thenStatusForbidden() throws Exception {
        //creating two shop two with owner/admin and two terminal in repos
        Account admin1 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop1 = utilPopulate.createShopForAdmin(admin1);
        Terminal terminal1 = utilPopulate.createTerminalForShop(shop1, admin1);

        Account admin2 = utilPopulate.createUser(Authority.ADMIN);
        Shop shop2 = utilPopulate.createShopForAdmin(admin2);
        Terminal terminal2 = utilPopulate.createTerminalForShop(shop2, admin2);

        //sending terminal not belonging to authenticated admin
        mockMvc.perform(delete("/terminals/" + terminal2.getId())
                        .with(user(admin1.getUsername()).password(admin1.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin1))))
                //@ExceptionHandler catches IdValidationException
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IdValidationException))
                .andExpect(status().isForbidden())
                //response with error.html
                .andExpect(view().name("error"));
        //terminal should not be deleted from repo
        assertEquals(2, terminalRepository.findAll().size());
    }

}
