package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.account.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Terminal extends AbstractPersistable<Long> {

    @NotEmpty(message = "TID cannot be empty")
    @Size(min = 8, max = 8, message = "TID должен содержать 8 цифр" )
    private String tid;

    @Size(min = 12, max = 12, message = "MID должен содержать 12 цифр")
    private String mid;

    @Pattern(regexp = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$", message = "Неверный формат IP-адреса")
    private String ip;

    @Size(min = 3, max = 120, message = "Заголовок должен быть в формате: текст | текст")
    private String chequeHeader;

    @ManyToOne
    private Account account;

    @NotNull(message = "Укажите магазин")
    @ManyToOne
    private Shop shop;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "terminal")
    private List<Transaction> transactions = new ArrayList<>();
}
