package com.app.bank_acquiring.domain.account;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

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

    @NotEmpty(message = "Логин не может быть пустым")
    @Size(min = 8, max = 40, message = "Название должно содержать не больше 40 букв")
    private String username;

    @NotEmpty(message = "Пароль не может быть пустым")
    @Size(min = 8, message = "Слишком короткий пароль")
    private String password;

    @ManyToMany(mappedBy = "accounts")
    private List<Shop> shops;

    @Enumerated(EnumType.STRING)
    private Authority authority;

    @OneToMany(mappedBy = "account")
    private List<Terminal> terminals = new ArrayList<>();

    @OneToOne(cascade = CascadeType.REMOVE, mappedBy = "account")
    private AccountInfo accountInfo;

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
