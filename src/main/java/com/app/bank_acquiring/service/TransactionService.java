package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    Logger logger = LoggerFactory.getLogger(TransactionService.class);

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
    private SalesCounterService salesCounterService;
    @Autowired
    private EmailServiceComponent emailService;

    public TransactionDto makeTransactionOperation(String currentUser, TransactionDto transactionDto, Type transactionType) {
        Account user = accountService.findByUsername(currentUser);
        Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
        String cheque = "";
        boolean transactionStatus = false;
        if (uposService.makeOperation(terminal.getAccount().getId(), terminal.getShop().getId(),
                terminal.getTid(), transactionDto.getAmount(), transactionType)) {
            cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
            transactionStatus = uposService.defineTransactionStatus(cheque);
        }

        Transaction transaction = convertToTransaction(transactionDto, transactionStatus, terminal,
                user.getUsername(), transactionType);
        Map<Product, Double> prodToQuantity = getProductsWithChangedAmount(transactionDto.getProductsList(),
                transactionDto.getProductsAmountList(), currentUser, transactionType);

        salesCounterService.addTransaction(transaction, terminal.getTid());
        transaction.setCheque(salesCounterService.getOperationTransactionToString(transaction, terminal, prodToQuantity)
                + cheque);//нужно будет убрать чек при неуспешной операции
        productCart.getProductsWithAmount().clear();
        return convertToDto(transactionRepository.save(transaction));
    }

    public TransactionDto makeReportOperation(String currentUser, Type transactionType) {
        Account user = accountService.findByUsername(currentUser);
        Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
        String cheque = "";
        boolean transactionStatus = false;
        if (uposService.makeReportOperation(terminal.getAccount().getId(), terminal.getShop().getId(),
                terminal.getTid(), transactionType)) {
            cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
            transactionStatus = uposService.defineTransactionStatus(cheque);
        }
        Transaction transaction = new Transaction();
        transaction.setStatus(transactionStatus);
        transaction.setType(transactionType);
        transaction.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        transaction.setTerminal(terminal);
        transaction.setCashier(currentUser);
        transaction.setCheque(salesCounterService.getReportOperationToString(transaction, terminal) + cheque);//нужно будет убрать чек при неуспешной операции
        if (transactionStatus && transactionType == Type.CLOSE_DAY) {
            salesCounterService.closeDay(terminal.getTid());
        }
        return convertToDto(transactionRepository.save(transaction));
    }


    @Transactional
    private Map<Product, Double> getProductsWithChangedAmount(List<Long> productIdsList, List<Double> prodAmountList,
                                                              String currentUser, Type transactionType) {
        Map<Product, Double> prodToQuantity = new HashMap<>();
        for (int i = 0; i < productIdsList.size(); i++) {
            Product product = productService.getProduct(productIdsList.get(i), currentUser);
            double prodAmount = prodAmountList.get(i);
            double balance = product.getBalance();
            if (transactionType == Type.PAYMENT) {
                balance -= prodAmount;
            } else if (transactionType == Type.REFUND) {
                balance += prodAmount;
            }
            if (balance >= 0) {
                product.setBalance(balance);
                prodToQuantity.put(product, prodAmount);
            }
        }
        return prodToQuantity;
    }


    public List<String> getSalesStatistics(String currentUser) {
        String userWorkingTid = accountService.findByUsername(currentUser).getWorkTerminalTid();
        if (currentUser == null || userWorkingTid == null) {
            return null;
        }
        SalesCounter salesCounter = salesCounterService.getSalesStatistics(userWorkingTid);
        DecimalFormat df = new DecimalFormat("0.00");
        if (salesCounter != null && salesCounter.getTerminalTid().equals(userWorkingTid)) {
            List<String> list = new ArrayList<>();
            list.add("В кассе: " + df.format(salesCounter.getBalancePerDay()));
            list.add("Продажи: " + df.format(salesCounter.getSalesPerDay()) + "(" + salesCounter.getSalesCounterPerDay() + ")");
            list.add("Возвраты: " + df.format(salesCounter.getRefundsPerDay()) + "(" + salesCounter.getRefundsCounterPerDay() + ")");
            return list;
        }
        return null;
    }

    public boolean sendEmail(String currentUser, List<String> emailToCheque) {
        if (accountService.findByUsername(currentUser) == null) {
            return false;
        }
        try {
            emailService.sendMail(emailToCheque.get(0), emailToCheque.get(1));
            logger.info("Cheque was sent to " + emailToCheque.get(0));
            return true;
        } catch (Exception e) {
            logger.error("Cannot send cheque to email: " + e.getMessage());
            return false;
        }

    }

    private Transaction convertToTransaction(TransactionDto transactionDto, boolean status,
                                             Terminal terminal, String cashierName, Type type) {
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDto.getAmount());
        transaction.setStatus(status);
        transaction.setType(type);
        transaction.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        transaction.setTerminal(terminal);
        transaction.setCashier(cashierName);
        return transaction;
    }


    public TransactionDto convertToDto(Transaction entity) {
        TransactionDto dto = new TransactionDto();
        dto.setStatus(entity.getStatus());
        dto.setCheque(entity.getCheque());
        return dto;
    }
}
