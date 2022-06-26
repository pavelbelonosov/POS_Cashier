package com.app.bank_acquiring.controller;


import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
public class TransactionRestController {

    @Autowired
    private TransactionService transactionService;

   /*@GetMapping("/api/v1/transactions/{terminalID}")
    public List<Transaction> getTransactions(@PathVariable int terminalID,
                                             @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        return transactionRepository.findByTerminal(terminalRepository.findByTid(terminalID));
    }*/


    @PostMapping("/api/v1/transactions/pay")
    public TransactionDto makePayment(@RequestBody TransactionDto transactionDto,
                                      @AuthenticationPrincipal UserDetails currentUser) {
       return transactionService.makePayment(currentUser,transactionDto);
    }

}
