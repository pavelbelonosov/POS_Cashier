package com.app.bank_acquiring.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Terminal extends AbstractPersistable<Long> {

    private int tid;
    private String ip;
    @ManyToOne
    private Account account;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "terminal")
    private List<Transaction> transactions = new ArrayList<>();
}
