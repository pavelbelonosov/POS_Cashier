package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TerminalService {
    private final Logger logger = LoggerFactory.getLogger(TerminalService.class);
    private TerminalRepository terminalRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UposService uposService;


    @Transactional
    public void addTerminalToAccount(@NonNull Terminal terminal, @NonNull String currentUser) {
        Account account = accountRepository.findByUsername(currentUser);
        terminal.setAccount(account);
        terminalRepository.save(terminal);
        account.getTerminals().add(terminal);
        if (!uposService.createUserUpos(account.getId(), terminal)) {
            logger.error("UPOS not created for the account(id " + account.getId() + ")");
            throw new RuntimeException("Error while creating UPOS for the account");
        }
    }

    @Transactional
    public void updateTerminal(Long id, String currentUser, @NonNull String ip, @NonNull String chequeHeader) {
        Terminal terminal = getValidatedTerminal(id, currentUser);
        if (!ip.isBlank()) {
            terminal.setIp(ip);
        }
        if (!chequeHeader.isBlank()) {
            terminal.setChequeHeader(chequeHeader);
        }
        uposService.updateUposSettings(accountRepository.findByUsername(currentUser).getId(), terminal);
    }

    @Transactional
    public boolean testConnection(Long terminalId, String currentUser) {
        if (terminalId == null || currentUser == null || currentUser.isBlank()) {
            return false;
        }
        Account current = accountRepository.findByUsername(currentUser);
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
    public void deleteTerminal(@NonNull Long id, @NonNull String currentUser) {
        Terminal terminal = getValidatedTerminal(id, currentUser);
        uposService.deleteUserUpos(accountRepository.findByUsername(currentUser).getId(),
                terminal.getShop().getId(), terminal.getTid());
        getEmployeesWithThisWorkTerminal(terminal.getTid(), currentUser).forEach(account -> account.setWorkTerminalTid(null));
        terminalRepository.delete(terminal);
    }

    @Transactional
    public void setWorkTerminalToAccount(@NonNull String currentUser, @NonNull Long id) {
        Account current = accountRepository.findByUsername(currentUser);
        current.setWorkTerminalTid(terminalRepository.getOne(id).getTid());
    }

    public List<Account> getEmployeesWithThisWorkTerminal(@NonNull String terminalTid, @NonNull String currentUser) {
        Account owner = accountRepository.findByUsername(currentUser);
        //getting all employees, belonging to owner/admin via his shops
        List<List<Account>> listOfListsEmployees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = listOfListsEmployees.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        //filtering employees by having given terminal in work
        return employees.stream().map(account -> account.getId()).distinct()
                .map(id -> accountRepository.getOne(id))
                .filter(account -> account.getWorkTerminalTid() != null && account.getWorkTerminalTid().equals(terminalTid))
                .collect(Collectors.toList());
    }

    public Terminal getValidatedTerminal(Long id, String currentUser) {
        if (id == null || currentUser == null || currentUser.isBlank()) {
            return null;
        }
        Account user = accountRepository.findByUsername(currentUser);
        Terminal terminal = terminalRepository.getOne(id);
        validateIdAccess(user, terminal);
        return terminal;
    }

    public Terminal getTerminalByTid(String tid) {
        if (tid != null && !tid.isBlank() && tid.length() == 8) {
            return terminalRepository.findByTid(tid);
        }
        return null;
    }

    public void validateIdAccess(Account account, Terminal terminal) {
        if (terminal == null || account.getTerminals() == null || !account.getTerminals().contains(terminal)) {
            logger.error("ID validation error: given account(id "
                    + (account != null ? account.getId() : "")
                    + ") doesn't have permission to terminal(id "
                    + (terminal != null ? terminal.getId() : "") + ")");
            throw new RuntimeException("Current account doesn't have this terminal");
        }
    }

}
