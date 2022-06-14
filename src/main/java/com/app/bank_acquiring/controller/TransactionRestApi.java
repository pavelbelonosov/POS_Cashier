package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.Transaction;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.repository.TransactionRepository;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
public class TransactionRestApi {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UposService uposService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TerminalRepository terminalRepository;

   /*@GetMapping("/api/v1/transactions/{terminalID}")
    public List<Transaction> getTransactions(@PathVariable int terminalID,
                                             @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        return transactionRepository.findByTerminal(terminalRepository.findByTid(terminalID));
    }*/

    @GetMapping("/api/v1/transactions/{id}")
    public Transaction getTransaction(@PathVariable Long id) {
        return transactionRepository.getOne(id);
    }

    @PostMapping("/api/v1/transactions/pay")
    public Transaction makePayment(@RequestBody Transaction transaction,
                                   @AuthenticationPrincipal UserDetails currentUser)  {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.findByTid(user.getWorkTerminalTid());
        System.out.println(terminal.getTid()+" "+ terminal.getAccount().getUsername());
        String cheque = "";
        if (uposService.makePayment(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid(), transaction.getAmount())) {
            cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
        }
        transaction.setStatus(uposService.defineTransactionStatus(cheque));
        transaction.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        transaction.setTerminal(terminal);
        transaction.setCheque(cheque);
        transaction.setCashier(user.getUsername());
        return transactionRepository.save(transaction);
    }
}
