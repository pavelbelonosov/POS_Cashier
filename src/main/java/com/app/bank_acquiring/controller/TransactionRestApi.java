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

import java.io.IOException;

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
                                   @AuthenticationPrincipal UserDetails currentUser) throws IOException, InterruptedException {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = user.getWorkingTerminal();
        String cheque = "";
        if (uposService.makePayment(user.getId(), terminal.getTid(), transaction.getAmount())){
            cheque = uposService.readCheque(user.getId(),terminal.getTid());
        }
        transaction.setStatus(uposService.defineTransactionStatus(cheque));
        transaction.setTerminal(terminal);
        transaction.setCheque(cheque);
        return transactionRepository.save(transaction);
    }
}
