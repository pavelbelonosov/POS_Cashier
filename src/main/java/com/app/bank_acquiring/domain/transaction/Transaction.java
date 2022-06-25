package com.app.bank_acquiring.domain.transaction;

import com.app.bank_acquiring.domain.Terminal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
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
    private Boolean status;
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    private Type type;
    private double amount;
    @Lob
    private String cheque = "";
    private String cashier;

    @ManyToOne
    @JsonIgnore
    private Terminal terminal;

}
