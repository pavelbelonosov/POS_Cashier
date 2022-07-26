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

    private Map<Product, Double> productsWithAmount = new HashMap<>();

    public Map<Product, Double> getProductsWithAmount() {
        return this.productsWithAmount;
    }

    public void addToCart(Product addedProduct, double amount) {
        Product existingProduct = getExistingProduct(addedProduct);
        if (existingProduct != null) {
            if (productsWithAmount.get(existingProduct) + amount > existingProduct.getBalance()) {
                productsWithAmount.put(existingProduct, existingProduct.getBalance() * 1.0);
                return;
            }
            if (productsWithAmount.get(existingProduct) + amount <= 0) {
                productsWithAmount.remove(existingProduct);
                return;
            }
            productsWithAmount.put(existingProduct, productsWithAmount.get(existingProduct) + amount);
        } else {
            if (amount > addedProduct.getBalance()) {
                productsWithAmount.put(addedProduct, addedProduct.getBalance() * 1.0);
                return;
            }
            productsWithAmount.put(addedProduct, amount);
        }

    }

    public Double getTotalPrice() {
        Double totalPrice = 0.0;
        for (Product p : productsWithAmount.keySet()) {
            totalPrice += p.getSellingPrice().doubleValue() * productsWithAmount.get(p);
        }
        return (int) (Math.round(totalPrice * 100)) / 100.0;
    }


    private Product getExistingProduct(Product product) {
        return productsWithAmount.keySet().stream().filter(productInMap -> productInMap.
                equals(product)).findFirst().orElse(null);
    }

}

