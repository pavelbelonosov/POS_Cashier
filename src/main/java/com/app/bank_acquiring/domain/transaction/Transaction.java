package com.app.bank_acquiring.domain.transaction;

import com.app.bank_acquiring.domain.Terminal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
    @org.hibernate.annotations.Type(type = "org.hibernate.type.TextType")
    private String cheque = "";

    private String cashier;

    @ManyToOne
    @JsonIgnore
    private Terminal terminal;

    @Override
    public void setId(Long id) {
        if (id != null) super.setId(id);
    }

    @Override
    @EqualsAndHashCode.Include
    public Long getId() {
        return super.getId();
    }

}
