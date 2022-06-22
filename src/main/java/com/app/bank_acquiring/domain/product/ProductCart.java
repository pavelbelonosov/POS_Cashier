package com.app.bank_acquiring.domain.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;


@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProductCart implements Serializable {

    private Map<Product, Integer> products = new HashMap<>();

    public Map<Product, Integer> getProducts() {
        return this.products;
    }

    public void addToCart(Product product, int amount) {
        Product product1 = products.keySet().stream().filter(key -> key.getId().equals(product.getId())).findFirst().orElse(null);
        if (product1 != null) {
            products.put(product1, products.get(product1) + amount);
        } else {
            products.put(product, amount);
        }

    }

}

