package com.app.bank_acquiring.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesCounter extends AbstractPersistable<Long> {

    private int salesCounterPerDay;
    private double salesPerDay;
    private int refundsCounterPerDay;
    private double refundsPerDay;
    private double balancePerDay;
    private double salesAll;
    private int shift;
    @Column(unique = true)
    private String terminalTid;

}
