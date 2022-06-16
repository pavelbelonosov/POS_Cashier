package com.app.bank_acquiring.domain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class TransactionDto {

    private Long id;
    private boolean status;
    private LocalDateTime dateTime;
    private double amount;
    private String cheque;

    public TransactionDto(Long id, boolean status, LocalDateTime dateTime, double amount, String cheque){
        this.id=id;
        this.status=status;
        this.amount=amount;
        this.dateTime=dateTime;
        this.cheque=cheque;
    }
}
