package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.SalesCounterRepository;
import com.app.bank_acquiring.service.SalesCounterService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;

@ActiveProfiles("test")
public class SalesCounterServiceTest {

    private SalesCounterService salesCounterService;
    private SalesCounterRepository salesCounterRepository = Mockito.mock(SalesCounterRepository.class);
    private SalesCounter salesCounter = Mockito.mock(SalesCounter.class);

    @Before
    public void setUp() {
        salesCounterService = new SalesCounterService(salesCounterRepository);
    }

    @Test
    public void givenFirstTerminalTransaction_whenAddTransaction_thenNewSalesCounterIsSavedInRepository() {
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction transaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(transaction, terminalTid);

        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        Mockito.verify(salesCounterRepository, Mockito.times(1)).save(valueCapture.capture());
        SalesCounter salesCounter = valueCapture.getValue();

        assertNotNull(salesCounter);
        assertTrue(salesCounter.getTerminalTid().equals(terminalTid));
        assertTrue(salesCounter.getSalesPerDay() == transaction.getAmount()
                && salesCounter.getSalesCounterPerDay() == 1);
    }


    @Test
    public void givenTwoConsecutivePaymentTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction firstTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(firstTransaction, terminalTid);

        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter salesCounter = valueCapture.getValue();

        assertTrue(salesCounter.getSalesPerDay() == firstTransaction.getAmount());
        assertTrue(salesCounter.getSalesCounterPerDay() == 1);
        assertTrue(salesCounter.getBalancePerDay() == firstTransaction.getAmount());

        //second transaction
        Transaction secondTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(salesCounter);
        salesCounterService.addTransaction(secondTransaction, terminalTid);
        assertTrue(salesCounter.getSalesPerDay() == firstTransaction.getAmount() + secondTransaction.getAmount());
        assertTrue(salesCounter.getSalesPerDay() == 2);
        assertTrue(salesCounter.getBalancePerDay() == firstTransaction.getAmount() + secondTransaction.getAmount());

    }


    @Test
    public void givenTwoRefundTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction firstTransaction = createTransactionForTerminal(Type.REFUND, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(firstTransaction, terminalTid);

        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter salesCounter = valueCapture.getValue();

        assertTrue(salesCounter.getRefundsPerDay() == firstTransaction.getAmount());
        assertTrue(salesCounter.getRefundsCounterPerDay() == 1);
        assertTrue(salesCounter.getBalancePerDay() == -firstTransaction.getAmount());

        //second transaction
        Transaction secondTransaction = createTransactionForTerminal(Type.REFUND, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(salesCounter);
        salesCounterService.addTransaction(secondTransaction, terminalTid);

        assertTrue(salesCounter.getRefundsPerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount()));
        assertTrue(salesCounter.getRefundsCounterPerDay() == 2);
        assertTrue(salesCounter.getBalancePerDay() == -(firstTransaction.getAmount() + secondTransaction.getAmount()));
    }

    @Test
    public void givenOnePaymentAndOneRefundTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction paymentTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(paymentTransaction, terminalTid);

        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter salesCounter = valueCapture.getValue();

        assertTrue(salesCounter.getSalesPerDay() == paymentTransaction.getAmount());
        assertTrue(salesCounter.getSalesCounterPerDay() == 1);
        assertTrue(salesCounter.getBalancePerDay() == paymentTransaction.getAmount());

        //second transaction
        Transaction refundTransaction = createTransactionForTerminal(Type.REFUND, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(salesCounter);
        salesCounterService.addTransaction(refundTransaction, terminalTid);

        assertTrue(salesCounter.getRefundsPerDay() == refundTransaction.getAmount());
        assertTrue(salesCounter.getRefundsCounterPerDay() == 1);
        assertTrue(salesCounter.getSalesPerDay() == paymentTransaction.getAmount());
        assertTrue(salesCounter.getSalesCounterPerDay() == 1);
        assertTrue(salesCounter.getBalancePerDay() == (paymentTransaction.getAmount() - refundTransaction.getAmount()));
    }

    @Test
    public void whenCloseDay_thenSalesCounterIncrementShiftAndAddDayBalanceToSalesAll() {
        ArgumentCaptor<Integer> valueCapture = ArgumentCaptor.forClass(Integer.class);
        Terminal terminal = createTerminal();
        Mockito.when(salesCounterRepository.findByTerminalTid(terminal.getTid())).thenReturn(this.salesCounter);
        Mockito.when(this.salesCounter.getShift()).thenReturn(0);
        Mockito.when(this.salesCounter.getSalesPerDay()).thenReturn(1.10);
        salesCounterService.closeDay(terminal.getTid());

        Mockito.verify(this.salesCounter).setShift(valueCapture.capture());
        assertTrue(valueCapture.getValue() == 1);
        Mockito.verify(this.salesCounter).setSalesAll(valueCapture.capture());
        assertTrue(Double.parseDouble(valueCapture.getValue() + "") == 1.10);
    }

    @Test
    public void givenNonNullArg_whenGetReportOperation_thenReturnNotEmptyString() {
        Terminal terminal = createTerminal();
        Transaction closeDay = createTransactionForTerminal(Type.CLOSE_DAY, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminal.getTid())).thenReturn(this.salesCounter);
        Mockito.when(terminal.getShop()).thenReturn(new Shop());
        String zCheque = salesCounterService.getReportOperationToString(closeDay, terminal);
        assertFalse(zCheque.isEmpty());
        assertTrue(zCheque.contains("ОТЧЕТ О ЗАКРЫТИИ СМЕНЫ"));

        Transaction xReport = createTransactionForTerminal(Type.XREPORT, terminal);
        String xCheque = salesCounterService.getReportOperationToString(xReport, terminal);
        assertFalse(xCheque.isEmpty());
        assertTrue(xCheque.contains("ПРОМЕЖУТОЧНЫЙ ОТЧЕТ"));
    }

    @Test
    public void givenNonNullArgs_whenGetOperationTransaction_thenReturnNotEmptyString() {
        Terminal terminal = createTerminal();
        Transaction payment = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminal.getTid())).thenReturn(this.salesCounter);
        Mockito.when(terminal.getShop()).thenReturn(new Shop());
        String paymentCheque = salesCounterService.getOperationTransactionToString(payment, terminal, new HashMap<>());
        assertFalse(paymentCheque.isEmpty());
        assertTrue(paymentCheque.contains("Приход"));

        Transaction refund = createTransactionForTerminal(Type.REFUND, terminal);
        String refundCheque = salesCounterService.getOperationTransactionToString(refund, terminal, new HashMap<>());
        assertFalse(refundCheque.isEmpty());
        assertTrue(refundCheque.contains("Возврат прихода"));
    }

    private Transaction createTransactionForTerminal(Type transactionType, Terminal terminal) {
        Transaction transaction = new Transaction();
        transaction.setType(transactionType);
        transaction.setDateTime(LocalDateTime.now());
        transaction.setCheque("cheque");
        transaction.setAmount(1.10);
        transaction.setTerminal(terminal);
        return transaction;
    }

    private Terminal createTerminal() {
        Terminal terminal = spy(Terminal.class);
        terminal.setTid("12345678");
        return terminal;
    }

}
