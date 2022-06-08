package com.app.bank_acquiring.domain.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.validation.constraints.Email;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo extends AbstractPersistable<Long> {



    private String firstName;

    private String lastName;
    @Email
    private String email;

    private String telephoneNumber;

    @OneToOne
    @MapsId
    private Account account;

}
