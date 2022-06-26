package com.app.bank_acquiring.domain.transaction;

import com.app.bank_acquiring.domain.product.Product;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
public class TransactionDto {

    private long id;
    private Boolean status;
    private LocalDateTime dateTime;
    private double amount;
    private String cheque;
    private String cashierName;
    private List<Long> productsList;
    private List<Double> productsAmountList;

    public TransactionDto(Long id, boolean status, LocalDateTime dateTime,
                          Double amount, String cheque, String cashierName, List<Long> products, List<Double> productsAmountList){
        this.productsList = products;
        this.productsAmountList = productsAmountList;
        this.id=id;
        this.status=status;
        this.amount=amount;
        this.dateTime=dateTime;
        this.cheque=cheque;
        this.cashierName = cashierName;
    }
}
