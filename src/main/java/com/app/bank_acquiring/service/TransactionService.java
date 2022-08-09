package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.TransactionDto;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
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
@AllArgsConstructor
public class TransactionService {

    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private AccountService accountService;
    private TerminalService terminalService;
    private UposService uposService;
    private ProductService productService;
    private ProductCartComponent productCart;
    private TransactionRepository transactionRepository;
    private SalesCounterService salesCounterService;
    private EmailServiceComponent emailService;

    public TransactionDto makeTransactionOperation(String currentUser, TransactionDto transactionDto, Type transactionType) {
        if (currentUser != null && !currentUser.isBlank()) {
            Account user = accountService.findByUsername(currentUser);
            Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
            //performing acquiring operation
            String cheque = "";
            boolean transactionStatus = false;
            Map<Product, Double> prodToQuantity = new HashMap<>();
            //if terminal is standalone ->skip UPOS operation -> change products balance
            if (!terminal.getStandalone()) {
                if (uposService.makeOperation(terminal.getAccount().getId(), terminal.getShop().getId(),
                        terminal.getTid(), transactionDto.getAmount(), transactionType)) {
                    cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
                    transactionStatus = uposService.defineTransactionStatus(cheque);
                    if(transactionStatus){
                        //update products' balance in db: decrease or increase due to transaction type - payment or refund, and getting sold products
                        prodToQuantity = changeProductsAmountInRepository(transactionDto.getProductsList(),
                                transactionDto.getProductsAmountList(), currentUser, transactionType);
                    }

                    //if exception while parsing upos transaction cheque from system or upos doesnt perform cheque
                    if (cheque.isEmpty()) {
                        cheque = "Ошибка считывания банковского слипа";
                    }
                }
            } else {
                //update products' balance in db: decrease or increase due to transaction type - payment or refund, and getting sold products
                prodToQuantity = changeProductsAmountInRepository(transactionDto.getProductsList(),
                        transactionDto.getProductsAmountList(), currentUser, transactionType);
                //when bank pos is not involved transaction always successes
                transactionStatus = true;
            }
            //initializing transaction to persist in db, failed operations also saved
            Transaction transaction = convertToTransaction(transactionDto, transactionStatus, terminal,
                    user.getUsername(), transactionType);
            //updating sales statistics of given terminal
            salesCounterService.addTransaction(transaction, terminal.getTid());
            //obtaining operation cheque
            transaction.setCheque(salesCounterService.getOperationTransactionToString(transaction, terminal, prodToQuantity)
                    + cheque);//нужно будет убрать чек при неуспешной операции
            //evicting products from cart after operation
            productCart.getProductsWithAmount().clear();
            return convertToDto(transactionRepository.save(transaction));
        }
        return new TransactionDto();
    }

    public TransactionDto makeReportOperation(String currentUser, Type transactionType) {
        if (currentUser != null && !currentUser.isBlank()) {
            Account user = accountService.findByUsername(currentUser);
            Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
            //performing acquiring report operation
            String cheque = "";
            boolean transactionStatus = false;
            if (uposService.makeReportOperation(terminal.getAccount().getId(), terminal.getShop().getId(),
                    terminal.getTid(), transactionType)) {
                cheque = uposService.readCheque(terminal.getAccount().getId(), terminal.getShop().getId(), terminal.getTid());
                transactionStatus = uposService.defineTransactionStatus(cheque);
            }
            //initializing transaction to persist in db, failed operations also saved
            Transaction transaction = new Transaction();
            transaction.setStatus(transactionStatus);
            transaction.setType(transactionType);
            transaction.setDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            transaction.setTerminal(terminal);
            transaction.setCashier(currentUser);
            transaction.setCheque(salesCounterService.getReportOperationToString(transaction, terminal) + cheque);//нужно будет убрать чек при неуспешной операции
            //when shift closed successfully, sales counter of given terminal is reset
            if (transactionStatus && transactionType == Type.CLOSE_DAY) {
                salesCounterService.closeDay(terminal.getTid());
            }
            return convertToDto(transactionRepository.save(transaction));
        }
        return new TransactionDto();
    }


    //@Transactional
    //@CacheEvict(value = "products", allEntries = true)
    private Map<Product, Double> changeProductsAmountInRepository(List<Long> productIdsList, List<Double> prodAmountToSaleList,
                                                                  String currentUser, Type transactionType) {
        Map<Product, Double> prodToQuantity = new HashMap<>();
        for (int i = 0; i < productIdsList.size(); i++) {
            Product product = productService.getProduct(productIdsList.get(i), currentUser);
            double prodAmount = prodAmountToSaleList.get(i);
            double balance = product.getBalance();
            if (transactionType == Type.PAYMENT) {
                balance -= prodAmount;
            } else if (transactionType == Type.REFUND) {
                balance += prodAmount;
            }
            if (balance < 0) continue;
            product.setBalance(balance);
            prodToQuantity.put(product, prodAmount);
            productService.saveProduct(product);
        }
        return prodToQuantity;
    }


    public List<String> getSalesStatistics(String currentUser) {
        String userWorkingTid = accountService.findByUsername(currentUser).getWorkTerminalTid();
        List<String> list = new ArrayList<>();
        if (userWorkingTid != null) {
            SalesCounter salesCounter = salesCounterService.getSalesCounter(userWorkingTid);
            DecimalFormat df = new DecimalFormat("0.00");
            if (salesCounter != null && salesCounter.getTerminalTid().equals(userWorkingTid)) {
                list.add("В кассе: " + df.format(salesCounter.getBalancePerDay()));
                list.add("Продажи: " + df.format(salesCounter.getSalesPerDay()) + "(" + salesCounter.getSalesCounterPerDay() + ")");
                list.add("Возвраты: " + df.format(salesCounter.getRefundsPerDay()) + "(" + salesCounter.getRefundsCounterPerDay() + ")");
            }
        }
        return list;
    }

    public boolean sendEmail(String currentUser, List<String> emailAddressAndCheque) {
        if (accountService.findByUsername(currentUser) == null) {
            return false;
        }
        try {
            emailService.sendMail(emailAddressAndCheque.get(0), emailAddressAndCheque.get(1));
            logger.info("Cheque was sent to: " + emailAddressAndCheque.get(0));
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
        dto.setCashierName(entity.getCashier());
        dto.setAmount(entity.getAmount());
        dto.setDateTime(entity.getDateTime());
        return dto;
    }

}
