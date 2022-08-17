package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.junit.Assert.*;
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
    private ShopService shopService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TerminalService terminalService;
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
    public void givenAdmin_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.PAYMENT);
        assertEquals(transactions.get(0).getCashier(), admin.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.PAYMENT);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakePayment_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/pay")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.PAYMENT);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.REFUND);
        assertEquals(transactions.get(0).getCashier(), admin.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.REFUND);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakeRefund_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(post("/api/v1/transactions/refund")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
                        //sending dto to server
                        .content(asJsonString(dto))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertEquals(transactions.get(0).getAmount(), dto.getAmount(), 0.0);
        assertSame(transactions.get(0).getType(), Type.REFUND);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenSendChequeToEmail_thenStatusOkAndReturnErrorValue() throws Exception {
        //creating admin
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin)))
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
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
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
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        List<String> clientAnswer = List.of("email@adress.ru", "chequeString");

        mockMvc.perform(post("/api/v1/transactions/mailcheque")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee)))
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
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.CLOSE_DAY);
        assertEquals(transactions.get(0).getCashier(), admin.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenCloseDay_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.CLOSE_DAY);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenCloseDay_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/closeday")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.CLOSE_DAY);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.XREPORT);
        assertEquals(transactions.get(0).getCashier(), admin.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenHeadCashier_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.XREPORT);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenCashier_whenMakeXreport_thenNewTransactionSavedInDB() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //transaction repo should be empty at the beginning
        assertEquals(0, transactionRepository.findAll().size());

        mockMvc.perform(get("/api/v1/transactions/xreport")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                //standalone pos -> transaction successful
                .andExpect(jsonPath("$.status").value("true"))
                //cheque content
                .andExpect(jsonPath("$.cheque").isNotEmpty());
        //even failed transaction should be saved in db
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        assertSame(transactions.get(0).getType(), Type.XREPORT);
        assertEquals(transactions.get(0).getCashier(), employee.getUsername());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
    }

    @Test
    public void givenAdmin_whenGetTransactionStatistics_thenStatusOkAndReturnsStat() throws Exception {
        //creating scenario: admin->shop->product->terminal
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
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
                                .authorities(utilPopulate.getAuthorities(admin))))
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
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.HEAD_CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //making one payment operation to init sales counter for this terminal
        transactionService.makeTransactionOperation(employee.getUsername(), dto,
                com.app.bank_acquiring.domain.transaction.Type.PAYMENT);

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
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
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //creating employee for shop and setting terminal in work for this employee
        Account employee = utilPopulate.createUser(Authority.CASHIER);
        shopService.bundleShopWithAccount(shop, employee.getUsername());
        terminalService.setWorkTerminalToAccount(employee.getUsername(), terminal.getId());
        //creating dto from client
        TransactionDto dto = createDtoFromClient(product);
        //making one payment operation to init sales counter for this terminal
        transactionService.makeTransactionOperation(employee.getUsername(), dto,
                com.app.bank_acquiring.domain.transaction.Type.PAYMENT);

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(employee.getUsername()).password(employee.getPassword())
                                .authorities(utilPopulate.getAuthorities(employee))))
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
        Account admin = utilPopulate.createUser(Authority.ADMIN);
        Shop shop = utilPopulate.createShopForAdmin(admin);
        Product product = utilPopulate.createProductForShop(shop);
        Terminal terminal = utilPopulate.createDetachedTerminalForShop(shop);
        //saving terminal in db with attaching to admin
        terminalService.addTerminalToAccount(terminal, admin.getUsername());
        //setting terminal in work for admin
        terminalService.setWorkTerminalToAccount(admin.getUsername(), terminal.getId());

        mockMvc.perform(get("/api/v1/transactions/stat")
                        .with(user(admin.getUsername()).password(admin.getPassword())
                                .authorities(utilPopulate.getAuthorities(admin))))
                .andExpect(status().isNotFound());
        //clearing upos files from system
        shopService.deleteShop(shop.getId(), admin.getUsername());
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
