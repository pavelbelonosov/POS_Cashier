package com.app.bank_acquiring.controller;


import com.app.bank_acquiring.domain.SalesStatistics;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
        return transactionService.makeTransactionOperation(currentUser.getUsername(), transactionDto, Type.PAYMENT);
    }

    @PostMapping("/api/v1/transactions/refund")
    public TransactionDto makeRefund(@RequestBody TransactionDto transactionDto,
                                     @AuthenticationPrincipal UserDetails currentUser) {
        return transactionService.makeTransactionOperation(currentUser.getUsername(), transactionDto, Type.REFUND);
    }

    @GetMapping("/api/v1/transactions/closeday")
    public TransactionDto closeDay(@AuthenticationPrincipal UserDetails currentUser){
        return transactionService.makeReportOperation(currentUser.getUsername(),Type.CLOSE_DAY);
    }

    @GetMapping("/api/v1/transactions/xreport")
    public TransactionDto makeXreport(@AuthenticationPrincipal UserDetails currentUser){
        return transactionService.makeReportOperation(currentUser.getUsername(),Type.XREPORT);
    }

    @GetMapping("/api/v1/transactions/stat")
    public ResponseEntity<List<String>> getTransactionStatistics(@AuthenticationPrincipal UserDetails currentUser){
        List<String> stat = transactionService.getSalesStatistics(currentUser.getUsername());
        return stat==null?new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND):
                new ResponseEntity<List<String>>(stat, HttpStatus.OK);
    }

}
