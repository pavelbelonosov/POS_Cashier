package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.SalesCounter;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import com.app.bank_acquiring.repository.SalesCounterRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.Map;

@Service
@AllArgsConstructor
public class SalesCounterService {

    private final DecimalFormat df = new DecimalFormat("0.00");

    private SalesCounterRepository salesCounterRepository;

    public void addTransaction(Transaction transaction, String terminalTid) {
        if(!transaction.getStatus()) return;
        SalesCounter salesCounter = getSalesCounter(terminalTid);
        if (salesCounter == null) {
            salesCounter = new SalesCounter();
            salesCounter.setTerminalTid(terminalTid);
        }
        switch (transaction.getType()) {
            case PAYMENT:
                salesCounter.setSalesCounterPerDay(salesCounter.getSalesCounterPerDay() + 1);
                salesCounter.setSalesPerDay(salesCounter.getSalesPerDay() + transaction.getAmount());
                salesCounter.setBalancePerDay(salesCounter.getBalancePerDay() + transaction.getAmount());
                break;
            case REFUND:
                salesCounter.setRefundsCounterPerDay(salesCounter.getRefundsCounterPerDay() + 1);
                salesCounter.setRefundsPerDay(salesCounter.getRefundsPerDay() + transaction.getAmount());
                salesCounter.setBalancePerDay(salesCounter.getBalancePerDay() - transaction.getAmount());
                break;
        }
        saveSalesCounter(salesCounter);
    }

    public String getOperationTransactionToString(Transaction transaction, Terminal terminal,
                                                  Map<Product, Double> prodToQuantity) {
        if (transaction == null || terminal == null) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        SalesCounter salesCounter = getSalesCounter(terminal.getTid());

        s.append(terminal.getShop().getName() + "\n" + terminal.getShop().getCity() + " " + terminal.getShop().getAddress() + "\n");
        s.append("Смена: №" + (salesCounter != null ? salesCounter.getShift() : "")
                + ", Чек: №" + (salesCounter != null ? (salesCounter.getSalesCounterPerDay() + salesCounter.getRefundsCounterPerDay()) : 0) + "\n");
        s.append("Кассир " + transaction.getCashier() + "\n");
        s.append((transaction.getType() == Type.PAYMENT ? "Приход " : "Возврат прихода ")
                + transaction.getDateTime().toString().replace("T", " ") + "\n");
        s.append("ТОВАРНЫЙ ЧЕК\n");
        int i = 1;
        for (Product product : prodToQuantity.keySet()) {
            double amount = prodToQuantity.get(product);
            s.append(i + ". " + product.getName() + "(" + product.getMeasurementUnit().getShortExplanation() + ")\n"
                    + amount + "X" + product.getSellingPrice() + "..........."
                    + df.format(amount * product.getSellingPrice().doubleValue()) + "\n");
            i++;
        }
        s.append("ИТОГ: " + transaction.getAmount() + "\n");
        if(!terminal.getStandalone()) s.append("Безналичными: " + transaction.getAmount() + "\n");
        s.append("Спасибо за покупку!\n");
        s.append("\n");
        return s.toString();
    }

    public String getReportOperationToString(Transaction transaction, Terminal terminal) {
        if (transaction == null || terminal == null || terminal.getShop() == null) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        SalesCounter salesCounter = getSalesCounter(terminal.getTid());
        s.append(transaction.getType() == Type.CLOSE_DAY ? "ОТЧЕТ О ЗАКРЫТИИ СМЕНЫ\n" : "ПРОМЕЖУТОЧНЫЙ ОТЧЕТ\n");
        s.append("МЕСТО РАСЧЕТОВ " + terminal.getShop().getName() + "\n" + terminal.getShop().getCity()
                + " " + terminal.getShop().getAddress() + "\n");
        s.append("Смена №" + (salesCounter != null ? salesCounter.getShift() : "") + "\n");
        s.append("ЧЕКОВ ЗА СМЕНУ "
                + (salesCounter != null ? (salesCounter.getSalesCounterPerDay() + salesCounter.getRefundsCounterPerDay()) : 0) + "\n");
        s.append("Кассир " + transaction.getCashier() + "\n");
        s.append("ПРИХОД " + df.format(salesCounter != null ? salesCounter.getSalesPerDay() : 0) + "\n");
        s.append("ВОЗВРАТ ПРИХОДА " + df.format(salesCounter != null ? salesCounter.getRefundsPerDay() : 0) + "\n");
        s.append("ВЫРУЧКА  " + df.format(salesCounter != null ? salesCounter.getBalancePerDay() : 0) + "\n");
        s.append("НЕОБНУЛЯЕМАЯ СУММА ПРИХОДА " + df.format(salesCounter != null ? salesCounter.getSalesAll() : 0) + "\n");
        s.append("\n");
        return s.toString();
    }

    public void closeDay(String terminalTid) {
        SalesCounter salesCounter = getSalesCounter(terminalTid);
        if (salesCounter != null) {
            salesCounter.setSalesCounterPerDay(0);
            salesCounter.setRefundsCounterPerDay(0);
            salesCounter.setRefundsPerDay(0);
            salesCounter.setBalancePerDay(0);

            salesCounter.setShift(salesCounter.getShift() + 1);
            salesCounter.setSalesAll(salesCounter.getSalesAll() + salesCounter.getSalesPerDay());
            salesCounter.setSalesPerDay(0);
            saveSalesCounter(salesCounter);
        }
    }

    public SalesCounter getSalesCounter(String terminalTid) {
        return terminalTid != null ? salesCounterRepository.findByTerminalTid(terminalTid) : null;
    }

    public SalesCounter saveSalesCounter(SalesCounter salesCounter) {
        return salesCounter != null ? salesCounterRepository.save(salesCounter) : new SalesCounter();
    }

}
