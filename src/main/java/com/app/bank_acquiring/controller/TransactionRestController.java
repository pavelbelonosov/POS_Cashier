package com.app.bank_acquiring.controller;


import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@AllArgsConstructor
public class TransactionRestController {

    private TransactionService transactionService;

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

    @PostMapping("/api/v1/transactions/mailcheque")
    public ResponseEntity<String> sendChequeToEmail(@RequestBody List<String> emailWithCheque,
                                                    @AuthenticationPrincipal UserDetails currentUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        return transactionService.sendEmail(currentUser.getUsername(), emailWithCheque) ?
                new ResponseEntity<>("{\"error\":\"0\"}", headers, HttpStatus.OK)
                : new ResponseEntity<>("{\"error\":\"1\"}", headers, HttpStatus.OK);
    }

    @GetMapping("/api/v1/transactions/closeday")
    public TransactionDto closeDay(@AuthenticationPrincipal UserDetails currentUser) {
        return transactionService.makeReportOperation(currentUser.getUsername(), Type.CLOSE_DAY);
    }

    @GetMapping("/api/v1/transactions/xreport")
    public TransactionDto makeXreport(@AuthenticationPrincipal UserDetails currentUser) {
        return transactionService.makeReportOperation(currentUser.getUsername(), Type.XREPORT);
    }

    @GetMapping("/api/v1/transactions/stat")
    public ResponseEntity<List<String>> getTransactionStatistics(@AuthenticationPrincipal UserDetails currentUser) {
        List<String> stat = transactionService.getSalesStatistics(currentUser.getUsername());
        return stat == null ? new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(stat, HttpStatus.OK);
    }

}
