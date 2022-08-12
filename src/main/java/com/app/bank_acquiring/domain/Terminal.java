package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.transaction.Transaction;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Terminal extends AbstractPersistable<Long> {


    @NotEmpty(message = "TID не может быть пустым")
    @NotBlank(message = "TID не может состоть из пробелов")
    @Pattern(regexp = "^[0-9]{8}$", message = "Неверный формат TID")
    @Column(unique = true)
    @EqualsAndHashCode.Include
    private String tid;

    @NotNull
    private Boolean standalone; //интегрированный терминал vs атономный

    @Pattern(regexp = "^[0-9]{12}$", message = "Неверный формат MID")
    @EqualsAndHashCode.Include
    private String mid;

    //@NotNull(message = "IP не может быть пустым")
    @Pattern(regexp = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$", message = "Неверный формат IP-адреса")
    private String ip;

    @Size(min = 3, max = 120, message = "Заголовок должен быть в формате: текст | текст")
    private String chequeHeader;

    @ManyToOne
    private Account account;

    @NotNull(message = "Укажите магазин")
    @ManyToOne
    @EqualsAndHashCode.Include
    private Shop shop;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "terminal")
    private List<Transaction> transactions = new ArrayList<>();

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
