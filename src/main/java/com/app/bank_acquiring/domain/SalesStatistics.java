package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Data
@NoArgsConstructor
public class SalesStatistics {
    private final DecimalFormat df = new DecimalFormat("0.00");
    private int salesCounter;
    private double sales;
    private int refundsCounter;
    private double refunds;
    private double currentBalance;
    private Terminal terminal;
    private Transaction transaction;
    private Map<Product, Double> prodToQuantity = new HashMap<>();

    public double addTransaction(Transaction transaction, Map<Product, Double> prodToQuantity,
                                 Terminal terminal, Type transactionType) {
        this.prodToQuantity = prodToQuantity;
        this.terminal = terminal;
        this.transaction = transaction;
        if (transactionType == Type.REFUND) {
            refundsCounter++;
            refunds += transaction.getAmount();
            return currentBalance -= transaction.getAmount();
        }
        salesCounter++;
        sales += transaction.getAmount();
        return currentBalance += transaction.getAmount();
    }

    public String getOperationTransactionToString(Type transactionType) {
        StringBuilder s = new StringBuilder();
        s.append(terminal.getShop().getName() + "\n" + terminal.getShop().getCity() + " " + terminal.getShop().getAddress() + "\n");
        s.append("Смена: №" + terminal.getShiftCounter() + ", Чек: №" + (salesCounter + refundsCounter) + "\n");
        s.append("Кассир " + transaction.getCashier() + "\n");
        s.append((transactionType == Type.PAYMENT ? "Приход " : "Возврат прихода ") + transaction.getDateTime().toString().replace("T", " ") + "\n");
        s.append("ТОВАРНЫЙ ЧЕК\n");
        int i = 1;
        for (Product product : prodToQuantity.keySet()) {
            double amount = prodToQuantity.get(product);
            s.append(i + ". " + product.getName() + "(" + product.getMeasurementUnit().getShortExplanation() + ")\n"
                    + amount + "X" + product.getSellingPrice() + "..........."
                    + (amount * product.getSellingPrice().doubleValue()) + "\n");
            i++;
        }
        s.append("ИТОГ: " + transaction.getAmount() + "\n");
        s.append("Безналичными: " + transaction.getAmount() + "\n");
        s.append("Спасибо за покупку!\n");
        s.append("\n");
        return s.toString();
    }

    public String getReportOperationToString(Type transactionType) {
        StringBuilder s = new StringBuilder();
        s.append(transactionType == Type.CLOSE_DAY ? "ОТЧЕТ О ЗАКРЫТИИ СМЕНЫ\n" : "ПРОМЕЖУТОЧНЫЙ ОТЧЕТ\n");
        s.append("МЕСТО РАСЧЕТОВ " + (terminal!=null?terminal.getShop().getName():"")
                + "\n" + (terminal!=null?terminal.getShop().getCity():"")
                + " " + (terminal!=null?terminal.getShop().getAddress():"") + "\n");
        s.append("Смена №" + (terminal!=null?terminal.getShiftCounter():"") + "\n");
        s.append("ЧЕКОВ ЗА СМЕНУ " + (salesCounter + refundsCounter)+"\n");
        s.append("Кассир " + (transaction!=null?transaction.getCashier():"") + "\n");
        s.append("ПРИХОД (БЕЗНАЛИЧНЫМИ) " + df.format(sales) + "\n");
        s.append("ВОЗВРАТ ПРИХОДА (БЕЗНАЛИЧНЫМИ) " + df.format(refunds) + "\n");
        s.append("ВЫРУЧКА  " + df.format(currentBalance) + "\n");
        s.append("\n");
        return s.toString();
    }

    public void clear() {
        this.salesCounter = 0;
        this.refundsCounter = 0;
        this.sales = 0;
        this.refunds = 0;
        this.currentBalance = 0;
        this.prodToQuantity.clear();
        this.terminal = null;
        this.transaction = null;
    }
}
