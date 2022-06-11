package com.app.bank_acquiring.domain.account;

import javax.persistence.*;
import javax.validation.constraints.*;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account extends AbstractPersistable<Long> {

    @NotNull(message = "Логин не может быть пустым")
    @NotBlank(message = "Логин не может быть пустым")
    @Size(min = 8,max=40,message = "Логин от 8 до 40 символов")
    private String username;

    @NotNull(message = "Пароль не может быть пустым")
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Слишком короткий пароль")
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

    public Terminal getWorkingTerminal() {
        try {
            return terminals.stream()
                    .filter(terminal -> terminal.getTid().equals(workTerminalTid))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
