package com.app.bank_acquiring.unitTest;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.SalesCounterRepository;
import com.app.bank_acquiring.service.SalesCounterService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class SalesCounterServiceTest {

    @Autowired
    private SalesCounterService salesCounterService;
    @Autowired
    private SalesCounterRepository salesCounterRepository;

    @After
    public void tearDown() {
        salesCounterRepository.deleteAll();
    }

    @Test
    public void givenTransaction_whenAddTransaction_thenSalesCounterIsSavedInRepository() {
        Transaction transaction = createTransactionForTerminal(Type.PAYMENT);
        String terminalTid = transaction.getTerminal().getTid();
        salesCounterService.addTransaction(transaction, terminalTid);
        SalesCounter salesCounter = salesCounterRepository.findByTerminalTid(terminalTid);
        assertNotNull(salesCounter);
        assertTrue(salesCounter.getSalesPerDay() == transaction.getAmount()
                && salesCounter.getSalesCounterPerDay() == 1);
    }

    @Test
    public void givenTwoPaymentTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        Transaction firstTransaction = createTransactionForTerminal(Type.PAYMENT);
        String terminalTid = firstTransaction.getTerminal().getTid();
        salesCounterService.addTransaction(firstTransaction, terminalTid);
        SalesCounter salesCounter1 = getSalesCounter(terminalTid);
        assertTrue(salesCounter1.getSalesPerDay() == firstTransaction.getAmount()
                && salesCounter1.getSalesCounterPerDay() == 1
                && salesCounter1.getBalancePerDay() == firstTransaction.getAmount());

        Transaction secondTransaction = createTransactionForTerminal(Type.PAYMENT);
        salesCounterService.addTransaction(secondTransaction, terminalTid);
        SalesCounter salesCounter2 = getSalesCounter(terminalTid);
        assertTrue(salesCounter2.getSalesPerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount())
                && salesCounter2.getSalesCounterPerDay() == 2
                && salesCounter2.getBalancePerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount()));
    }

    @Test
    public void givenTwoRefundTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        Transaction firstTransaction = createTransactionForTerminal(Type.REFUND);
        String terminalTid = firstTransaction.getTerminal().getTid();
        salesCounterService.addTransaction(firstTransaction, terminalTid);
        SalesCounter salesCounter1 = getSalesCounter(terminalTid);
        assertTrue(salesCounter1.getRefundsPerDay() == firstTransaction.getAmount()
                && salesCounter1.getRefundsCounterPerDay() == 1
                && salesCounter1.getBalancePerDay() == -firstTransaction.getAmount());

        Transaction secondTransaction = createTransactionForTerminal(Type.REFUND);
        salesCounterService.addTransaction(secondTransaction, terminalTid);
        SalesCounter salesCounter2 = getSalesCounter(terminalTid);
        assertTrue(salesCounter2.getRefundsPerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount())
                && salesCounter2.getRefundsCounterPerDay() == 2
                && salesCounter2.getBalancePerDay() == -(firstTransaction.getAmount() + secondTransaction.getAmount()));
    }

    @Test
    public void givenOneRefundAndOnePaymentTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        Transaction firstTransaction = createTransactionForTerminal(Type.PAYMENT);
        String terminalTid = firstTransaction.getTerminal().getTid();
        salesCounterService.addTransaction(firstTransaction, terminalTid);
        SalesCounter salesCounter1 = getSalesCounter(terminalTid);
        assertTrue(salesCounter1.getSalesPerDay() == firstTransaction.getAmount()
                && salesCounter1.getSalesCounterPerDay() == 1
                && salesCounter1.getBalancePerDay() == firstTransaction.getAmount());

        Transaction secondTransaction = createTransactionForTerminal(Type.REFUND);
        salesCounterService.addTransaction(secondTransaction, terminalTid);
        SalesCounter salesCounter2 = getSalesCounter(terminalTid);
        assertTrue(salesCounter2.getRefundsPerDay() == secondTransaction.getAmount()
                && salesCounter2.getRefundsCounterPerDay() == 1
                && salesCounter2.getBalancePerDay() == (firstTransaction.getAmount() - secondTransaction.getAmount()));
    }

    @Test
    public void whenCloseDay_thenSalesCounterIsReset() {
        Transaction transaction = createTransactionForTerminal(Type.PAYMENT);
        String terminalTid = transaction.getTerminal().getTid();
        salesCounterService.addTransaction(transaction, terminalTid);
        SalesCounter salesCounter1 = getSalesCounter(terminalTid);
        assertTrue(salesCounter1.getShift() == 0);

        salesCounterService.closeDay(terminalTid);
        SalesCounter salesCounter2 = getSalesCounter(terminalTid);
        assertTrue(salesCounter2.getSalesCounterPerDay() == 0
                && salesCounter2.getBalancePerDay() == 0
                && salesCounter2.getRefundsCounterPerDay() == 0
                && salesCounter2.getSalesPerDay() == 0
                && salesCounter2.getRefundsPerDay() == 0);
        assertTrue(salesCounter2.getShift() == 1
                && salesCounter2.getSalesAll() == transaction.getAmount());
    }

    private Transaction createTransactionForTerminal(Type transactionType) {
        Transaction transaction = new Transaction();
        transaction.setType(transactionType);
        transaction.setCheque("cheque");
        transaction.setAmount(1.10);
        transaction.setTerminal(createTerminal());
        return transaction;
    }

    private Terminal createTerminal() {
        Terminal terminal = new Terminal();
        terminal.setTid("12345678");
        return terminal;
    }

    private SalesCounter getSalesCounter(String terminalTid) {
        return salesCounterRepository.findByTerminalTid(terminalTid);
    }
}
