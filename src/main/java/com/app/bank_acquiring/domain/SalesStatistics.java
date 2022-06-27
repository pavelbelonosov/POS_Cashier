package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.transaction.Transaction;
import com.app.bank_acquiring.domain.transaction.Type;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Data
@NoArgsConstructor
public class SalesStatistics {

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
        currentBalance += transaction.getAmount();
        if (transactionType==Type.REFUND) {
            refundsCounter++;
            return refunds += transaction.getAmount();
        }
        salesCounter++;
        return sales += transaction.getAmount();
    }

    public String getOperationTransactionToString(Type transactionType) {
        StringBuilder s = new StringBuilder();
        s.append(terminal.getShop().getName() + "\n" + terminal.getShop().getCity() + " " + terminal.getShop().getAddress() + "\n");
        s.append("Смена: №" + terminal.getShiftCounter() + ", Чек: №" + (salesCounter + refundsCounter) + "\n");
        s.append("Кассир " + transaction.getCashier() + "\n");
        s.append(transactionType==Type.PAYMENT?"Приход ":"Возврат прихода " + transaction.getDateTime() + "\n");
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

}
