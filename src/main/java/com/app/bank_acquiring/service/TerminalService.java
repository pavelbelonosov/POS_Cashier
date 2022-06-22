package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TerminalService {

    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UposService uposService;


    @Transactional
    public void addTerminalToAccount(Terminal terminal, UserDetails currentUser) throws IOException {
        Account account = accountRepository.findByUsername(currentUser.getUsername());
        terminal.setAccount(account);
        terminalRepository.save(terminal);
        account.getTerminals().add(terminal);
        uposService.createUserUpos(account.getId(), terminal);
    }

    @Transactional
    public void updateTerminal(Long id, UserDetails currentUser, String ip, String chequeHeader) {
        Terminal terminal = getValidatedTerminal(id, currentUser);
        if (!ip.isEmpty()) {
            terminal.setIp(ip);
        }
        if (!chequeHeader.isEmpty()) {
            terminal.setChequeHeader(chequeHeader);
        }
        //terminalRepository.save(terminal);
        uposService.updateUposSettings(accountRepository.findByUsername(currentUser.getUsername()).getId(), terminal);
    }

    @Transactional
    public boolean testConnection(Long terminalId, UserDetails currentUser) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(terminalId);
        validateIdAccess(current, terminal);
        if (uposService.testPSDB(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid())) {
            Transaction test = new Transaction();
            String testCheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
            test.setStatus(uposService.defineTransactionStatus(testCheque));
            test.setType(Type.TEST);
            test.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            test.setCheque(testCheque);
            test.setTerminal(terminal);
            transactionRepository.save(test);
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteTerminal(Long id, UserDetails currentUser) {
        Terminal terminal = getValidatedTerminal(id, currentUser);
        uposService.deleteUserUpos(accountRepository.findByUsername(currentUser.getUsername()).getId(),
                terminal.getShop().getId(), terminal.getTid());
        terminalRepository.delete(terminal);
    }

    @Transactional
    public void setWorkTerminalToAccount(UserDetails currentUser, Long id) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        current.setWorkTerminalTid(terminalRepository.getOne(id).getTid());
    }

    public List<Account> getAccountsWithWorkTerminal(Terminal terminal, UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        List<List<Account>> listOfListsEmployees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = listOfListsEmployees.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return employees.stream().filter(account -> account.getWorkTerminalTid().equals(terminal.getTid())).collect(Collectors.toList());
    }

    public Terminal getValidatedTerminal(Long id, UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(id);
        validateIdAccess(user, terminal);
        return terminal;
    }

    private void validateIdAccess(Account account, Terminal terminal) {
        if (!account.getTerminals().contains(terminal)) {
            throw new RuntimeException("Current account doesn't have this terminal");
        }
    }

}
