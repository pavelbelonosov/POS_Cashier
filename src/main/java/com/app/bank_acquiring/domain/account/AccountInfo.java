package com.app.bank_acquiring.domain.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo extends AbstractPersistable<Long> {


    @Size(max = 50, message = "Слишком длинное имя")
    private String firstName;

    @Size(max = 50, message = "Слишком длинная фамилия")
    private String lastName;

    @Email(message = "Неверный формат почты")
    private String email;

    @Pattern(regexp = "^(([0-9]){10})$", message = "Неверный формат номера")
    private String telephoneNumber;

    @OneToOne
    @MapsId
    private Account account;

}
