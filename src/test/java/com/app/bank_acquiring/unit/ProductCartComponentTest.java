package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.service.ProductCartComponent;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductCartComponentTest {

    @Autowired
    private ProductCartComponent productCartComponent;
    @Autowired
    private ProductRepository productRepository;

    @After
    public void tearDown() {
        productCartComponent.getProductsWithAmount().clear();
    }

    @Test
    public void givenNewProductWithAppropriateAmount_whenAddToCart_thenAddProductWithGivenAmount() {
        Product product = createProductWithBalance();
        productCartComponent.addToCart(product, product.getBalance() - 1);
        assertTrue(productCartComponent.getProductsWithAmount().get(product) < product.getBalance());
    }

    @Test
    public void givenNewProductWithExcessiveAmount_whenAddToCart_thenAddProductWithMaxAmount() {
        Product product = createProductWithBalance();
        productCartComponent.addToCart(product, product.getBalance() + 1);
        assertTrue(productCartComponent.getProductsWithAmount().get(product).equals(product.getBalance()));
    }

    @Test
    public void givenExistingProductWithAppropriateAmount_whenAddToCart_thenIncreaseAmountInCart() {
        Product product = createProductWithBalance();
        putProductHalfAmountInCart(product);
        productCartComponent.addToCart(product, 1);
        assertTrue(productCartComponent.getProductsWithAmount().get(product).equals(product.getBalance() / 2 + 1));
    }

    @Test
    public void givenExistingProductWithExcessiveAmount_whenAddToCart_thenSetMaxAmountInCart() {
        Product product = createProductWithBalance();
        putProductHalfAmountInCart(product);
        productCartComponent.addToCart(product, product.getBalance() + 1);
        assertTrue(productCartComponent.getProductsWithAmount().get(product).equals(product.getBalance()));
    }

    @Test
    public void whenGetTotalPrice_thenReturnsNumber() {
        Product product1 = createProductWithBalance();
        Product product2 = createProductWithBalance();

        putProductHalfAmountInCart(product1);
        putProductHalfAmountInCart(product2);

        assertTrue(productCartComponent.getTotalPrice() > 0);
    }

    private Product createProductWithBalance() {
        Product product = new Product();
        product.setName("product");
        product.setSellingPrice( new BigDecimal("123.45"));
        product.setBalance(10.5);
        return productRepository.save(product);
    }

    private void putProductHalfAmountInCart(Product product) {
        productCartComponent.getProductsWithAmount().put(product, product.getBalance() / 2);
    }
}
