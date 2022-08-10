package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.TransactionRepository;
import com.app.bank_acquiring.service.*;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
public class TransactionServiceTest {

    private AccountService accountService = Mockito.mock(AccountService.class);
    private TerminalService terminalService = Mockito.mock(TerminalService.class);
    private UposService uposService = Mockito.mock(UposService.class);
    private ProductService productService = Mockito.mock(ProductService.class);
    private ProductCartComponent productCart = Mockito.mock(ProductCartComponent.class);
    private TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
    private SalesCounterService salesCounterService = Mockito.mock(SalesCounterService.class);
    private EmailServiceComponent emailService = Mockito.mock(EmailServiceComponent.class);
    private TransactionService transactionService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        mockUposServiceMethods();
        mockSalesCounterServiceMethods();
        mockProductsCartMethods();
        transactionService = new TransactionService(accountService, terminalService, uposService,
                productService, productCart, transactionRepository, salesCounterService, emailService);
    }

    @Test
    public void whenMakeTransactionOperation_thenTransactionIsConvertedFromDtoAndSavedInRepository() {
        //setting account with terminal to work with
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminal.setAccount(account);
        account.setWorkTerminalTid(terminal.getTid());

        Type transactionType = Type.PAYMENT;
        TransactionDto transactionDtoFromClient = createTransactionDtoFromClient();
        ArgumentCaptor<Transaction> valueCapture = ArgumentCaptor.forClass(Transaction.class);
        doAnswer(invocationOnMock -> invocationOnMock.getArgument(0))
                .when(transactionRepository).save(any(Transaction.class));
        TransactionDto transactionDtoFromServer = transactionService.makeTransactionOperation(account.getUsername(),
                transactionDtoFromClient, transactionType);

        Mockito.verify(transactionRepository).save(valueCapture.capture());
        Transaction createdInMethodTransaction = valueCapture.getValue();
        assertTrue(createdInMethodTransaction.getTerminal().equals(terminal));
        assertTrue(createdInMethodTransaction.getAmount() == transactionDtoFromClient.getAmount());
        assertTrue(createdInMethodTransaction.getType() == transactionType);
        assertTrue(createdInMethodTransaction.getCashier().equals(account.getUsername()));
        assertTrue(transactionDtoFromServer.getCheque().equals(createdInMethodTransaction.getCheque()));
        assertTrue(transactionDtoFromServer.getStatus().equals(createdInMethodTransaction.getStatus()));
    }

    @Test
    public void whenMakeReportOperation_thenNewTransactionIsCreatedAndSavedInRepository() {
        //setting account with terminal to work with
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminal.setAccount(account);
        account.setWorkTerminalTid(terminal.getTid());

        Type transactionType = Type.CLOSE_DAY;
        ArgumentCaptor<Transaction> valueCapture = ArgumentCaptor.forClass(Transaction.class);
        doAnswer(invocationOnMock -> invocationOnMock.getArgument(0))
                .when(transactionRepository).save(any(Transaction.class));
        TransactionDto transactionDtoFromServer = transactionService.makeReportOperation(account.getUsername(), transactionType);

        Mockito.verify(transactionRepository).save(valueCapture.capture());
        Transaction createdInMethodTransaction = valueCapture.getValue();
        assertTrue(createdInMethodTransaction.getTerminal().equals(terminal));
        assertTrue(createdInMethodTransaction.getType() == transactionType);
        assertTrue(createdInMethodTransaction.getCashier().equals(account.getUsername()));
        assertTrue(transactionDtoFromServer.getCheque().equals(createdInMethodTransaction.getCheque()));
        assertTrue(transactionDtoFromServer.getStatus().equals(createdInMethodTransaction.getStatus()));
    }

    @Test
    public void whenGetSalesStatistics_thenReturnsListWithStatistics(){
        //setting account with terminal to work with and mocking sales counter to this terminal
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminal.setAccount(account);
        account.setWorkTerminalTid(terminal.getTid());
        SalesCounter sc = new SalesCounter();
        sc.setTerminalTid(terminal.getTid());
        Mockito.when(salesCounterService.getSalesCounter(anyString())).thenReturn(sc);

        List<String> statistics = transactionService.getSalesStatistics(account.getUsername());
        assertTrue(!statistics.isEmpty());
    }

    @Test
    public void whenSendEmail_thenEmailServiceIsInvoked(){
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminal.setAccount(account);
        account.setWorkTerminalTid(terminal.getTid());
        doNothing().when(emailService).sendMail(anyString(),anyString());
        assertTrue(transactionService.sendEmail(account.getUsername(), List.of("email","cheque")));
    }

    private Account createUser() {
        Account user = spy(Account.class);
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setId(Math.abs(new Random().nextLong()));
        Mockito.when(accountService.findByUsername(user.getUsername())).thenReturn(user);
        Mockito.when(accountService.getAccountById(user.getId())).thenReturn(user);
        return user;
    }

    private Terminal createTerminal() {
        Terminal terminal = new Terminal();
        terminal.setTid("12345678");
        terminal.setIp("1.1.1.1");
        terminal.setStandalone(false);
        terminal.setMid("123456789000");
        terminal.setShop(createShop());
        terminal.setId(Math.abs(new Random().nextLong()));
        Mockito.when(terminalService.getTerminalByTid(terminal.getTid())).thenReturn(terminal);
        return terminal;
    }

    private Shop createShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        shop.setId(Math.abs(new Random().nextLong()));
        return shop;
    }

    private TransactionDto createTransactionDtoFromClient() {
        TransactionDto dto = new TransactionDto();
        dto.setAmount(1.10);
        dto.setProductsAmountList(new ArrayList<>());
        dto.setProductsList(new ArrayList<>());
        return dto;
    }

    private void mockUposServiceMethods() {
        Mockito.when(uposService.makeOperation(anyLong(), anyLong(), anyString(),anyDouble(),any(Type.class))).thenReturn(true);
        Mockito.when(uposService.makeReportOperation(anyLong(), anyLong(), anyString(),any(Type.class))).thenReturn(true);
        Mockito.when(uposService.createUserUpos(anyLong(), any(Terminal.class))).thenReturn(true);
        Mockito.when(uposService.updateUposSettings(anyLong(), any(Terminal.class))).thenReturn(true);
        Mockito.when(uposService.deleteUserUpos(anyLong(), anyLong(), anyString())).thenReturn(true);
        Mockito.when(uposService.readCheque(anyLong(), anyLong(), anyString())).thenReturn(" ");
        Mockito.when(uposService.defineTransactionStatus(anyString())).thenReturn(true);
        Mockito.when(uposService.testPSDB(anyLong(), anyLong(), anyString())).thenReturn(true);
    }

    private void mockSalesCounterServiceMethods() {
        doNothing().when(salesCounterService).addTransaction(any(Transaction.class), anyString());
        Mockito.when(salesCounterService.
                getOperationTransactionToString(any(Transaction.class), any(Terminal.class), anyMap())).thenReturn("cheque");
    }

    private void mockProductsCartMethods() {
        Mockito.when(productCart.getProductsWithAmount()).thenReturn(new HashMap<>());
    }

    //to mock @ManyToMany relation between Accounts and Shops
    private void mockUserWithEmptyShopList(Account account) {
        Mockito.when(account.getShops()).thenReturn(new ArrayList<>());
    }
}
