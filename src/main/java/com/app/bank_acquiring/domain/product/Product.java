package com.app.bank_acquiring.domain.product;

import com.app.bank_acquiring.domain.Shop;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends AbstractPersistable<Long> {

    @NotEmpty(message = "Название не может быть пустым")
    @NotBlank(message = "Название не может состоять из пробелов")
    @Size(min = 3, max = 100, message = "Допустимое название от 3-100 символов")
    @EqualsAndHashCode.Include
    private String name;

    @Enumerated(EnumType.STRING)
    @EqualsAndHashCode.Include
    private Type type;

    private long barCode;

    private String vendorCode;

    @DecimalMin(value = "0.00")
    @Digits(integer = 6, fraction = 2, message = "Неверный формат цены")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.00", inclusive = false)
    @DecimalMax(value = "1000000", message = "Превышена максимальная цена")
    @Digits(integer = 6, fraction = 2, message = "Неверный формат цены")
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @EqualsAndHashCode.Include
    private MeasurementUnit measurementUnit;

    private double balance;

    @ManyToOne
    @EqualsAndHashCode.Include
    private Shop shop;

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
