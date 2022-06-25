package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.ProductCartComponent;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.repository.TransactionRepository;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class TransactionRestController {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UposService uposService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductCartComponent productCart;

   /*@GetMapping("/api/v1/transactions/{terminalID}")
    public List<Transaction> getTransactions(@PathVariable int terminalID,
                                             @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        return transactionRepository.findByTerminal(terminalRepository.findByTid(terminalID));
    }*/

    @GetMapping("/api/v1/transactions/{id}")
    public TransactionDto getTransaction(@PathVariable Long id) {
        return convertToDto(transactionRepository.getOne(id));
    }

    @Transactional
    @PostMapping("/api/v1/transactions/pay")
    public TransactionDto makePayment(@RequestBody TransactionDto transactionDto,
                                      @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.findByTid(user.getWorkTerminalTid());
        Transaction transaction = new Transaction();
        String cheque = "";
        if (uposService.makePayment(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid(), transactionDto.getAmount())) {
            cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
        }
        transaction.setAmount(transactionDto.getAmount());
        transaction.setStatus(uposService.defineTransactionStatus(cheque));
        transaction.setType(Type.PAYMENT);
        transaction.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        transaction.setTerminal(terminal);
        transaction.setCheque(cheque);
        transaction.setCashier(user.getUsername());

        for (int i = 0; i < transactionDto.getProductsList().size(); i++) {
            Product product = productService.getProduct(transactionDto.getProductsList().get(i), currentUser);
            double oldBalance = product.getBalance();
            double newBalance = oldBalance - transactionDto.getProductsAmountList().get(i);
            if (newBalance >= 0) {
                product.setBalance(newBalance);
            }
        }
        /*transactionDto.getProductsList().forEach(p -> {
            System.out.println("start");
            Product product = productService.getProduct(p, currentUser);
            double oldBalance = product.getBalance();
            System.out.println("sssss");
            if (oldBalance - v >= 0) {
                System.out.println("eeeeeeeeeee");
                product.setBalance(oldBalance - v);
                productService.saveProduct(product);
                System.out.println("ok");
            }
        });*/
        productCart.getProducts().clear();
        return convertToDto(transactionRepository.save(transaction));
    }

    protected TransactionDto convertToDto(Transaction entity) {
        TransactionDto dto = new TransactionDto(entity.getId(), entity.getStatus(), entity.getDateTime(),
                entity.getAmount(), entity.getCheque(), new ArrayList<>(), new ArrayList<>());
        return dto;
    }
}
