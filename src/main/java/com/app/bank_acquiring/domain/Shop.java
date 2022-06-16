package com.app.bank_acquiring.domain;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop extends AbstractPersistable<Long> {

    @ManyToMany
    private List<Account> accounts;

    @NotEmpty(message = "Название магазина не может быть пустым")
    @NotBlank(message = "Название магазина не может состоять из пробелов")
    @Size(max = 40, message = "Название должно содержать не больше 40 букв")
    private String name;
    @Size(max = 40, message = "Слишком длинное название города")
    private String city;
    @Size(max = 60, message = "Слишком длинный адрес")
    private String address;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "shop")
    private List<Terminal> terminals;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "shop")
    private List<Product> products;

}
