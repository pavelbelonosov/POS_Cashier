package com.app.bank_acquiring.domain.transaction;

import com.app.bank_acquiring.domain.product.Product;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
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

}
