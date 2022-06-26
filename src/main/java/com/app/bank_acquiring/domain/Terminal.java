package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Transaction;
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


    @NotEmpty(message = "TID не может быть пустым")
    @NotBlank(message = "TID не может состоть из пробелов")
    @Pattern(regexp = "^[0-9]{8}$", message = "Неверный формат TID")
    private String tid;

    @Pattern(regexp = "^[0-9]{12}$", message = "Неверный формат MID")
    private String mid;

    @NotNull(message = "IP не может быть пустым")
    @Pattern(regexp = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$", message = "Неверный формат IP-адреса")
    private String ip;

    @Size(min = 3, max = 120, message = "Заголовок должен быть в формате: текст | текст")
    private String chequeHeader;

    private int shiftCounter;

    @ManyToOne
    private Account account;

    @NotNull(message = "Укажите магазин")
    @ManyToOne
    private Shop shop;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "terminal")
    private List<Transaction> transactions = new ArrayList<>();



}
