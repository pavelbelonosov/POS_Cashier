package com.app.bank_acquiring.domain;

import javax.persistence.*;

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

    private String username;
    private String password;
    //private String shop;
    private String authority;
    @OneToMany(mappedBy = "account")
    private List<Terminal> terminals = new ArrayList<>();
    private int workTerminalTid;

    public Terminal getWorkingTerminal() {
        try {
            return terminals.stream()
                    .filter(terminal -> terminal.getTid() == workTerminalTid)
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
