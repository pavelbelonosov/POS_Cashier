package com.app.bank_acquiring.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.app.bank_acquiring.domain.product.Product;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;


@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProductCartComponent implements Serializable {

    private Map<Product, Double> products = new HashMap<>();

    public Map<Product, Double> getProducts() {
        return this.products;
    }

    public void addToCart(Product addedProduct, double amount) {
        Product existingProduct = getExistingProduct(addedProduct);
        if (existingProduct != null) {
            if (products.get(existingProduct) + amount > existingProduct.getBalance()) {
                products.put(existingProduct, existingProduct.getBalance() * 1.0);
                return;
            }
            if (products.get(existingProduct) + amount <= 0) {
                products.remove(existingProduct);
                return;
            }
            products.put(existingProduct, products.get(existingProduct) + amount);
        } else {
            if (amount > addedProduct.getBalance()) {
                products.put(addedProduct, addedProduct.getBalance() * 1.0);
                return;
            }
            products.put(addedProduct, amount);
        }

    }

    public Double getTotalPrice() {
        Double totalPrice = 0D;
        for (Product p : products.keySet()) {
            totalPrice += p.getSellingPrice().doubleValue() * products.get(p);
        }
        return (int) (Math.round(totalPrice * 100)) / 100.0;
    }


    private Product getExistingProduct(Product product) {
        return products.keySet().stream().filter(key -> key.getId().
                equals(product.getId())).findFirst().orElse(null);
    }

}

