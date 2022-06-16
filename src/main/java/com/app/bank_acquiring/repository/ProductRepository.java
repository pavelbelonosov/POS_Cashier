package com.app.bank_acquiring.repository;

import com.app.bank_acquiring.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
}
