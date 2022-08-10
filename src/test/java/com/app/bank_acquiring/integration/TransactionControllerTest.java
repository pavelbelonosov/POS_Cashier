package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.repository.*;
import com.app.bank_acquiring.service.ShopService;
import com.app.bank_acquiring.service.TerminalService;
import com.app.bank_acquiring.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The test class for pos-transaction(payment, refund, close day etc.) functions via rest api. Doesn't require POS-terminal to be connected.
 * Logic:each test creates one shop with one terminal, one product, one admin and optionally one employee.
 * UPOS files copied by app internal "mechanism" from base dir and deleted after each test.
 * Standalone POS-type is used for tests to avoid upos process invocation.
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {


    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private ShopService shopService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private TerminalService terminalService;
    @Autowired
    private SalesCounterRepository salesCounterRepository;

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
        productRepository.deleteAll();
        transactionRepository.deleteAll();
        terminalRepository.deleteAll();
        salesCounterRepository.deleteAll();
    }

    @Test
    public void givenAdmin_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.PAYMENT);
        assertTrue(transactions.get(0).getCashier().equals(admin.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.PAYMENT);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.PAYMENT);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.REFUND);
        assertTrue(transactions.get(0).getCashier().equals(admin.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.REFUND);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getAmount() == dto.getAmount());
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.REFUND);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenSendChequeToEmail_thenStatusOkAndReturnErrorValue() throws Exception {
        //creating admin
        Account admin = createUserInRepository(Authority.ADMIN);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin)))
                        //sending data to server
                        .content(asJsonString(clientAnswer))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //if exception don"t occur -> error:0, otherwise error:1
                .andExpect(jsonPath("$.error").value("0"));
    }

    @Test
    public void givenHeadCashier_whenSendChequeToEmail_thenStatusOkAndReturnErrorValue() throws Exception {
        //creating admin
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending data to server
                        .content(asJsonString(clientAnswer))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //if exceptions don't occur -> error:0, otherwise error:1
                .andExpect(jsonPath("$.error").value("0"));
    }

    @Test
    public void givenCashier_whenSendChequeToEmail_thenStatusOkAndReturnErrorValue() throws Exception {
        //creating admin
        Account employee = createUserInRepository(Authority.CASHIER);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee)))
                        //sending data to server
                        .content(asJsonString(clientAnswer))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //if exceptions don't occur -> error:0, otherwise error:1
                .andExpect(jsonPath("$.error").value("0"));
    }

    @Test
    public void givenAdmin_whenCloseDay_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.CLOSE_DAY);
        assertTrue(transactions.get(0).getCashier().equals(admin.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenCloseDay_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.CLOSE_DAY);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenCloseDay_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.CLOSE_DAY);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.XREPORT);
        assertTrue(transactions.get(0).getCashier().equals(admin.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.XREPORT);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertTrue(transactionRepository.findAll().size() == 0);

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successfull
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() == 1);
        assertTrue(transactions.get(0).getType() == com.app.bank_acquiring.domain.transaction.Type.XREPORT);
        assertTrue(transactions.get(0).getCashier().equals(employee.getUsername()));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenGetTransactionStatistics_thenStatusOkAndReturnsStat() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //making one payment operation to init sales counter for this terminal
        transactionService.makeTransactionOperation(admin.getUsername(), dto, com.app.bank_acquiring.domain.transaction.Type.PAYMENT);

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //check content of json arr in server response, should contain one payment
                .andExpect(jsonPath("$[0]").value("В кассе: "
                        + (dto.getAmount() + "").replace('.', ',')))
                .andExpect(jsonPath("$[1]").value("Продажи: "
                        + (dto.getAmount() + "").replace('.', ',') + "(1)"))
                .andExpect(jsonPath("$[2]").value("Возвраты: 0,00(0)"));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenGetTransactionStatistics_thenStatusOkAndReturnsStat() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //making one payment operation to init sales counter for this terminal
        transactionService.makeTransactionOperation(employee.getUsername(), dto,
                com.app.bank_acquiring.domain.transaction.Type.PAYMENT);

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //check content of json arr in server response, should contain one payment
                .andExpect(jsonPath("$[0]").value("В кассе: "
                        + (dto.getAmount() + "").replace('.', ',')))
                .andExpect(jsonPath("$[1]").value("Продажи: "
                        + (dto.getAmount() + "").replace('.', ',') + "(1)"))
                .andExpect(jsonPath("$[2]").value("Возвраты: 0,00(0)"));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenGetTransactionStatistics_thenStatusOkAndReturnsStat() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = createUserInRepository(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //making one payment operation to init sales counter for this terminal
        transactionService.makeTransactionOperation(employee.getUsername(), dto,
                com.app.bank_acquiring.domain.transaction.Type.PAYMENT);

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //check content of json arr in server response, should contain one payment
                .andExpect(jsonPath("$[0]").value("В кассе: "
                        + (dto.getAmount() + "").replace('.', ',')))
                .andExpect(jsonPath("$[1]").value("Продажи: "
                        + (dto.getAmount() + "").replace('.', ',') + "(1)"))
                .andExpect(jsonPath("$[2]").value("Возвраты: 0,00(0)"));
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenNoPreviousTransactions_whenGetTransactionStatistics_thenStatusNotFound() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = createUserInRepository(Authority.ADMIN);
        Shop shop = createShopForAdminInRepository(admin);
        Product product = createProductForShopInRepository(shop);
        Terminal terminal = createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(getAuthorities(admin))))
                .andExpect(status().isNotFound());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
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

    private Product createProductForShopInRepository(Shop shop) {
        Product product = new Product();
        product.setShop(shop);
        product.setName("product");
        product.setType(Type.ITEM);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        product.setPurchasePrice(new BigDecimal("123.45"));
        product.setSellingPrice(new BigDecimal("123.45"));
        return productRepository.save(product);
    }

    private Terminal createDetachedTerminalForShop(Shop shop) {
        Terminal terminal = new Terminal();
        terminal.setTid("00000000");
        terminal.setStandalone(true);
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        terminal.setShop(shop);
        return terminal;
    }

    private List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return Arrays.asList(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }

    private TransactionDto createDtoFromClient(Product product) {
        TransactionDto transactionDto = new TransactionDto();
        //assuming only one product in cart
        transactionDto.setAmount(product.getPurchasePrice().doubleValue());
        transactionDto.setProductsList(List.of(product.getId()));
        transactionDto.setProductsAmountList(List.of(1.0));
        return transactionDto;
    }

    private String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
