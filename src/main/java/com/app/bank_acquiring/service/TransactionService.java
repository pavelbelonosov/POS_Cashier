package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.SalesStatistics;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.ProductCartComponent;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionService {

    @Autowired
    private AccountService accountService;
    @Autowired
    private TerminalService terminalService;
    @Autowired
    private UposService uposService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductCartComponent productCart;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private SalesStatistics salesStatistics;

    @Transactional
    public TransactionDto makePayment(UserDetails currentUser, TransactionDto transactionDto) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
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
        //transaction.setCheque(cheque);
        transaction.setCashier(user.getUsername());

        Map<Product, Double> prodToQuantity = new HashMap<>();
        for (int i = 0; i < transactionDto.getProductsList().size(); i++) {
            Product product = productService.getProduct(transactionDto.getProductsList().get(i), currentUser);
            double prodAmount = transactionDto.getProductsAmountList().get(i);
            double newBalance = product.getBalance() - prodAmount;
            if (newBalance >= 0) {
                product.setBalance(newBalance);
                prodToQuantity.put(product, prodAmount);
            }
        }

        salesStatistics.addTransaction(transaction,prodToQuantity,terminal);
        transaction.setCheque(salesStatistics.getPaymentTransactionToString()+cheque);
        productCart.getProducts().clear();
        return convertToDto(transactionRepository.save(transaction));
    }

    public TransactionDto convertToDto(Transaction entity) {
        TransactionDto dto = new TransactionDto();
        dto.setStatus(entity.getStatus());
        dto.setCheque(entity.getCheque());
        return dto;
    }
}
