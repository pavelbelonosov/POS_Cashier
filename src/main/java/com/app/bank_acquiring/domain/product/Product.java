package com.app.bank_acquiring.domain.product;

import com.app.bank_acquiring.domain.Shop;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product extends AbstractPersistable<Long> {

    @NotEmpty(message = "Название не может быть пустым")
    @NotBlank(message = "Название не может состоять из пробелов")
    @Size(min = 3, max = 100, message = "Допустимое название от 3 до 100 символов")
    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    private long barCode;

    private String vendorCode;

    @DecimalMin(value = "0.0")
    @Digits(integer=6, fraction=2,message = "Неверный формат цены")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "1000000", message = "Превышена максимальная цена")
    @Digits(integer=6, fraction=2, message = "Неверный формат цены")
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    private MeasurementUnit measurementUnit;

    @ManyToOne
    private Shop shop;

}
