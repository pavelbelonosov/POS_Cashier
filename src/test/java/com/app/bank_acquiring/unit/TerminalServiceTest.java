package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.TerminalService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

import static org.junit.Assert.*;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class TerminalServiceTest {

    @Autowired
    private TerminalService terminalService;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ShopRepository shopRepository;

    @After
    public void tearDown() {
        terminalRepository.deleteAll();
        accountRepository.deleteAll();
        shopRepository.deleteAll();
    }

    @Test
    public void whenAddTerminalToAccount_thenTerminalIsSavedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        assertNotNull(terminalRepository.findByTid(terminal.getTid()));
        assertTrue(terminalRepository.findByTid(terminal.getTid()).getAccount().getId().equals(account.getId()));
    }

    @Test
    public void whenUpdateTerminal_thenUpdatedTerminalIsSavedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        assertTrue(terminalRepository.findByTid(terminal.getTid()).getIp().equals("1.1.1.1"));

        terminalService.updateTerminal(terminal.getId(), account.getUsername(), "1.2.3.4", "header");
        assertTrue(terminalRepository.findByTid(terminal.getTid()).getIp().equals("1.2.3.4"));
    }

    @Test(expected = RuntimeException.class)
    public void givenIncorrectTerminal_whenUpdateTerminal_thenThrowsRuntimeException() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());

        terminalService.updateTerminal(createTerminal().getId(), account.getUsername(), "1.2.3.4", "newHeader");
    }

    @Test
    public void whenDeleteTerminal_thenTerminalIsDeletedFromRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.deleteTerminal(terminal.getId(), account.getUsername());
        assertTrue(terminalRepository.findById(terminal.getId()).isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void givenIncorrectTerminal_whenDeleteTerminal_thenThrowsRuntimeException() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.deleteTerminal(createTerminal().getId(), account.getUsername());
    }

    @Test
    public void whenSetWorkTerminalToAccount_thenTerminalIsUpdatedInRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        terminalService.setWorkTerminalToAccount(account.getUsername(), terminal.getId());
        assertTrue(accountRepository.findByUsername(account.getUsername()).getWorkTerminalTid().equals(terminal.getTid()));
    }

    @Test
    @Transactional
    public void whenGetValidatedTerminal_thenTerminalIsReturnedFromRepository() {
        Account account = createUser();
        Terminal terminal = createTerminal();
        terminalService.addTerminalToAccount(terminal, account.getUsername());
        assertNotNull(terminalService.getValidatedTerminal(terminal.getId(), account.getUsername()));
    }

    @Test(expected = RuntimeException.class)
    @Transactional
    public void givenIncorrectTerminal_whenGetValidatedTerminal_thenThrowsRuntimeException() {
        Account account = createUser();
        Terminal terminal = createTerminal();

        terminalService.addTerminalToAccount(terminal, account.getUsername());

        terminalService.getValidatedTerminal(terminal.getId() + 1, account.getUsername());
    }

    private Account createUser() {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword("password");
        return accountRepository.save(user);
    }

    private Terminal createTerminal() {
        Terminal terminal = new Terminal();
        terminal.setTid("12345678");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setShop(createShop());
        return terminal;
    }

    private Shop createShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        return shopRepository.save(shop);
    }
}
