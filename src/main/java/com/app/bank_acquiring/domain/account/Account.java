package com.app.bank_acquiring.domain.account;

import javax.persistence.*;
import javax.validation.constraints.*;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.ArrayList;
import java.util.List;

@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account extends AbstractPersistable<Long> {

    @NotNull(message = "Логин не может быть пустым")
    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 8, max = 40, message = "Логин от 8 до 40 символов")
    @EqualsAndHashCode.Include
    private String username;

    @NotNull(message = "Пароль не может быть пустым")
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 8, message = "Слишком короткий пароль")
    @EqualsAndHashCode.Include
    private String password;

    @ManyToMany(mappedBy = "accounts")
    private List<Shop> shops;

    @Enumerated(EnumType.STRING)
    private Authority authority;

    @OneToMany(mappedBy = "account")
    private List<Terminal> terminals = new ArrayList<>();

    @OneToOne(mappedBy = "account")
    private AccountInfo accountInfo;

    @Pattern(regexp = "^[0-9]{8}$", message = "Неверный формат TID")
    private String workTerminalTid;

    @EqualsAndHashCode.Include
    private Long getAccountId() {
        if (getId() != null) {
            return getId();
        }
        return 0L;
    }
}
