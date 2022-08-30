package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.repository.TransactionRepository;
import com.app.bank_acquiring.service.IdValidationException;
import com.app.bank_acquiring.service.TerminalService;
import com.app.bank_acquiring.service.UposService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;


@ActiveProfiles("test")
public class TerminalServiceTest {


    private TerminalService terminalService;
    private TerminalRepository terminalRepository = Mockito.mock(TerminalRepository.class);
    private AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
    private TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
    private UposService uposService = Mockito.mock(UposService.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        mockUposServiceMethods();
        terminalService = new TerminalService(terminalRepository, accountRepository, transactionRepository, uposService);
    }

    @Test
    public void whenAddTerminalToAccount_thenTerminalIsSavedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        Mockito.verify(terminalRepository).save(terminal);
        assertTrue(account.getTerminals().contains(terminal));
        assertTrue(terminal.getAccount().equals(account));
    }

    @Test
    public void whenUpdateTerminal_thenUpdatedTerminalIsSavedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        assertTrue(terminal.getIp().equals("1.1.1.1"));
        assertTrue(terminal.getChequeHeader() == null);
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.updateTerminal(terminal.getId(), account.getUsername(), false, "1.2.3.4", "header");
        Mockito.verify(terminalRepository).save(terminal);
        assertTrue(terminal.getIp().equals("1.2.3.4"));
        assertTrue(terminal.getChequeHeader().equals("header"));
    }

    @Test
    public void givenIncorrectTerminal_whenUpdateTerminal_thenThrowsRuntimeException() {
        exceptionRule.expect(IdValidationException.class);
        exceptionRule.expectMessage("Current account doesn't have this terminal");

        Account account1 = createUser();
        Terminal terminal1 = createTerminal();
        terminalService.addTerminalToAccount(terminal1, account1.getUsername());
        Account account2 = createUser();
        Terminal terminal2 = createTerminal();
        terminalService.addTerminalToAccount(terminal2, account2.getUsername());
        //must throw RuntimeException due to ids' validation
        terminalService.updateTerminal(terminal1.getId(), account2.getUsername(), false," ", " ");
    }

    @Test
    public void givenBlankIpAndHeader_whenUpdateTerminal_thenChangesNotUpdated() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        assertTrue(terminal.getIp().equals("1.1.1.1"));
        assertTrue(terminal.getChequeHeader() == null);
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.updateTerminal(terminal.getId(), account.getUsername(),false, " ", " ");
        assertTrue(terminal.getIp().equals("1.1.1.1"));
        assertTrue(terminal.getChequeHeader() == null);
    }

    @Test
    public void whenTestConnection_thenNewTransactionIsSavedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        ArgumentCaptor<Transaction> valueCapture = ArgumentCaptor.forClass(Transaction.class);
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.testConnection(terminal.getId(), account.getUsername());

        Mockito.verify(transactionRepository).save(valueCapture.capture());
        Transaction test = valueCapture.getValue();
        assertTrue(test.getTerminal().equals(terminal));

    }

    @Test
    public void givenIncorrectIdAndUsername_whenTestConnection_thenThrowRuntimeException() {
        exceptionRule.expect(IdValidationException.class);
        exceptionRule.expectMessage("Current account doesn't have this terminal");

        Account account1 = createUser();
        Terminal terminal1 = createTerminal();
        terminalService.addTerminalToAccount(terminal1, account1.getUsername());
        Account account2 = createUser();
        Terminal terminal2 = createTerminal();
        terminalService.addTerminalToAccount(terminal2, account2.getUsername());
        //must throw RuntimeException due to ids' validation
        terminalService.testConnection(terminal1.getId(), account2.getUsername());
    }

    @Test
    public void givenNullIdAndUsername_whenTestConnection_thenThrowRuntimeException() {
        assertFalse(terminalService.testConnection(null, null));
    }

    @Test
    public void whenDeleteTerminal_thenTerminalIsDeletedFromRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        mockUserWithEmptyShopList(account);
        assertTrue(terminal.getId() > 0);
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.deleteTerminal(terminal.getId(), account.getUsername());
        Mockito.verify(terminalRepository).delete(terminal);
        assertTrue(terminal.getId() == -1L);
    }

    @Test
    public void givenIncorrectTerminal_whenDeleteTerminal_thenThrowsRuntimeException() {
        exceptionRule.expect(IdValidationException.class);
        exceptionRule.expectMessage("Current account doesn't have this terminal");

        Account account1 = createUser();
        Terminal terminal1 = createTerminal();
        terminalService.addTerminalToAccount(terminal1, account1.getUsername());
        Account account2 = createUser();
        Terminal terminal2 = createTerminal();
        terminalService.addTerminalToAccount(terminal2, account2.getUsername());
        //must throw RuntimeException due to ids' validation
        terminalService.deleteTerminal(terminal1.getId(), account2.getUsername());
    }

    @Test
    public void whenSetWorkTerminalToAccount_thenTerminalIsUpdated() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        assertTrue(account.getWorkTerminalTid() == null);
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.setWorkTerminalToAccount(account.getUsername(), terminal.getId());
        assertTrue(account.getWorkTerminalTid().equals(terminal.getTid()));
    }

    @Test
    public void whenGetValidatedTerminal_thenTerminalIsReturnedFromRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        assertEquals(terminal, terminalService.getValidatedTerminal(terminal.getId(), account.getUsername()));
    }
    @Test
    public void givenIncorrectTerminalIdAndUsername_whenGetValidatedTerminal_thenThrowsRuntimeException() {
        exceptionRule.expect(IdValidationException.class);
        exceptionRule.expectMessage("Current account doesn't have this terminal");

        Account account1 = createUser();
        Terminal terminal1 = createTerminal();
        terminalService.addTerminalToAccount(terminal1, account1.getUsername());
        Account account2 = createUser();
        Terminal terminal2 = createTerminal();
        terminalService.addTerminalToAccount(terminal2, account2.getUsername());
        //must throw RuntimeException due to ids' validation
        terminalService.getValidatedTerminal(terminal1.getId(), account2.getUsername());
    }

    private Account createUser() {
        Account user = spy(Account.class);
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        user.setId(Math.abs(new Random().nextLong()));
        Mockito.when(accountRepository.findByUsername(user.getUsername())).thenReturn(user);
        Mockito.when(accountRepository.getOne(user.getId())).thenReturn(user);
        doAnswer(invocationOnMock -> {
            Account arg = invocationOnMock.getArgument(0);
            arg.setId(-1L);
            return null;
        }).when(accountRepository).delete(any(Account.class));
        return user;
    }

    private Terminal createTerminal() {
        Terminal terminal = new Terminal();
        terminal.setTid("12345678");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setShop(createShop());
        terminal.setStandalone(false);
        terminal.setId(Math.abs(new Random().nextLong()));
        Mockito.when(terminalRepository.findByTid(terminal.getTid())).thenReturn(terminal);
        Mockito.when(terminalRepository.getOne(terminal.getId())).thenReturn(terminal);
        Mockito.when(terminalRepository.findById(terminal.getId())).thenReturn(Optional.of(terminal));
        doAnswer(invocationOnMock -> {
            Terminal arg = invocationOnMock.getArgument(0);
            arg.setId(-1L);
            return null;
        }).when(terminalRepository).delete(any(Terminal.class));
        return terminal;
    }

    private Shop createShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        shop.setId(Math.abs(new Random().nextLong()));
        return shop;
    }

    private void mockUposServiceMethods() {
        Mockito.when(uposService.createUserUpos(anyLong(), any(Terminal.class))).thenReturn(true);
        Mockito.when(uposService.updateUposSettings(anyLong(), any(Terminal.class))).thenReturn(true);
        Mockito.when(uposService.deleteUserUpos(anyLong(), anyLong(), anyString())).thenReturn(true);
        Mockito.when(uposService.readCheque(anyLong(), anyLong(), anyString())).thenReturn(" ");
        Mockito.when(uposService.defineTransactionStatusByCheque(anyString())).thenReturn(true);
        Mockito.when(uposService.testPSDB(anyLong(), anyLong(), anyString())).thenReturn(true);
    }

    //to mock @ManyToMany relation between Accounts and Shops
    private void mockUserWithEmptyShopList(Account account) {
        Mockito.when(account.getShops()).thenReturn(new ArrayList<>());
    }

}
