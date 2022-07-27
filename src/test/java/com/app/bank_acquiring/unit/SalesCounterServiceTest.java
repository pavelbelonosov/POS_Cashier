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
        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction firstTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(firstTransaction, terminalTid);

        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter firstTransactionSalesCounter = valueCapture.getValue();

        assertTrue(firstTransactionSalesCounter.getSalesPerDay() == firstTransaction.getAmount()
                && firstTransactionSalesCounter.getSalesCounterPerDay() == 1
                && firstTransactionSalesCounter.getBalancePerDay() == firstTransaction.getAmount());

        //second transaction
        Transaction secondTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        SalesCounter sc = copySalesCounter(firstTransactionSalesCounter);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(sc);
        salesCounterService.addTransaction(secondTransaction, terminalTid);

        Mockito.verify(salesCounterRepository, Mockito.times(2)).save(valueCapture.capture());
        SalesCounter secondTransactionSalesCounter = valueCapture.getValue();

        assertTrue(secondTransactionSalesCounter.getSalesPerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount())
                && secondTransactionSalesCounter.getSalesCounterPerDay() == 2
                && secondTransactionSalesCounter.getBalancePerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount()));
    }


    @Test
    public void givenTwoRefundTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction firstTransaction = createTransactionForTerminal(Type.REFUND, terminal);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(firstTransaction, terminalTid);

        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter firstTransactionSalesCounter = valueCapture.getValue();

        assertTrue(firstTransactionSalesCounter.getRefundsPerDay() == firstTransaction.getAmount()
                && firstTransactionSalesCounter.getRefundsCounterPerDay() == 1
                && firstTransactionSalesCounter.getBalancePerDay() == -firstTransaction.getAmount());

        //second transaction
        Transaction secondTransaction = createTransactionForTerminal(Type.REFUND, terminal);
        SalesCounter sc = copySalesCounter(firstTransactionSalesCounter);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(sc);
        salesCounterService.addTransaction(secondTransaction, terminalTid);

        Mockito.verify(salesCounterRepository, Mockito.times(2)).save(valueCapture.capture());
        SalesCounter secondTransactionSalesCounter = valueCapture.getValue();

        assertTrue(secondTransactionSalesCounter.getRefundsPerDay() == (firstTransaction.getAmount() + secondTransaction.getAmount())
                && secondTransactionSalesCounter.getRefundsCounterPerDay() == 2
                && secondTransactionSalesCounter.getBalancePerDay() == -(firstTransaction.getAmount() + secondTransaction.getAmount()));
    }

    @Test
    public void givenOnePaymentAndOneRefundTransactions_whenAddTransaction_thenSalesCounterCountsStatisticsRight() {
        ArgumentCaptor<SalesCounter> valueCapture = ArgumentCaptor.forClass(SalesCounter.class);
        //adding first transaction
        Terminal terminal = createTerminal();
        String terminalTid = terminal.getTid();
        Transaction paymentTransaction = createTransactionForTerminal(Type.PAYMENT, terminal);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(null);
        salesCounterService.addTransaction(paymentTransaction, terminalTid);

        Mockito.verify(salesCounterRepository).save(valueCapture.capture());
        SalesCounter firstTransactionSalesCounter = valueCapture.getValue();

        assertTrue(firstTransactionSalesCounter.getSalesPerDay() == paymentTransaction.getAmount()
                && firstTransactionSalesCounter.getSalesCounterPerDay() == 1
                && firstTransactionSalesCounter.getBalancePerDay() == paymentTransaction.getAmount());

        //second transaction
        Transaction refundTransaction = createTransactionForTerminal(Type.REFUND, terminal);
        SalesCounter sc = copySalesCounter(firstTransactionSalesCounter);

        Mockito.when(salesCounterRepository.findByTerminalTid(terminalTid)).thenReturn(sc);
        salesCounterService.addTransaction(refundTransaction, terminalTid);

        Mockito.verify(salesCounterRepository, Mockito.times(2)).save(valueCapture.capture());
        SalesCounter secondTransactionSalesCounter = valueCapture.getValue();

        assertTrue(secondTransactionSalesCounter.getRefundsPerDay() == refundTransaction.getAmount()
                && secondTransactionSalesCounter.getRefundsCounterPerDay() == 1
                && secondTransactionSalesCounter.getSalesPerDay() == paymentTransaction.getAmount()
                && secondTransactionSalesCounter.getSalesCounterPerDay() == 1
                && secondTransactionSalesCounter.getBalancePerDay() == (paymentTransaction.getAmount() - refundTransaction.getAmount()))
        ;
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
    public void givenNonNullArg_whenGetReportOperation_thenReturnNotEmptyString(){
        Terminal terminal = createTerminal();
        Transaction transaction = createTransactionForTerminal(Type.CLOSE_DAY, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminal.getTid())).thenReturn(this.salesCounter);
        Mockito.when(terminal.getShop()).thenReturn(new Shop());
        String cheque = salesCounterService.getReportOperationToString(transaction,terminal);
        assertFalse(salesCounterService.getReportOperationToString(transaction,terminal).isEmpty());
        assertTrue(cheque.contains("ОТЧЕТ О ЗАКРЫТИИ СМЕНЫ"));
    }

    @Test
    public void givenNonNullArgs_whenGetOperationTransaction_thenReturnNotEmptyString(){
        Terminal terminal = createTerminal();
        Transaction transaction = createTransactionForTerminal(Type.PAYMENT, terminal);
        Mockito.when(salesCounterRepository.findByTerminalTid(terminal.getTid())).thenReturn(this.salesCounter);
        Mockito.when(terminal.getShop()).thenReturn(new Shop());
        String cheque = salesCounterService.getOperationTransactionToString(transaction,terminal, new HashMap<>());
        assertFalse(cheque.isEmpty());
        assertTrue(cheque.contains("Приход"));
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


    private SalesCounter copySalesCounter(SalesCounter other) {
        SalesCounter sc = new SalesCounter();
        sc.setTerminalTid(other.getTerminalTid());
        sc.setSalesAll(other.getSalesAll());
        sc.setBalancePerDay(other.getBalancePerDay());
        sc.setShift(other.getShift());

        sc.setSalesPerDay(other.getSalesPerDay());
        sc.setSalesCounterPerDay(other.getSalesCounterPerDay());

        sc.setRefundsPerDay(other.getRefundsPerDay());
        sc.setRefundsCounterPerDay(other.getRefundsCounterPerDay());

        return sc;
    }
}
