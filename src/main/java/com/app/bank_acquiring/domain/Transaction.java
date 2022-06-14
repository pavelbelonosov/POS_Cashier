package com.app.bank_acquiring.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends AbstractPersistable<Long> {

    @NotNull
    private boolean status;
    private LocalDateTime dateTime;
    private double amount;
    @Lob
    private String cheque = "";
    private String cashier;
    @ManyToOne
    @JsonIgnore
    private Terminal terminal;

}
