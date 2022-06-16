package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AccountService accountService;

    @ModelAttribute
    public Product getProduct(){
        return new Product();
    }

    @GetMapping("/products")
    public String getProducts(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("account", user);
        model.addAttribute("typeService", Type.SERVICE);
        model.addAttribute("typeItem", Type.ITEM);
        model.addAttribute("measurementUnits", MeasurementUnit.values());
        return "products";
    }

    @GetMapping("/products/{id}")
    public String getProductById(Model model, @PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Product product = productRepository.getOne(id);
        model.addAttribute("product", product);
        return "product";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute Product product, BindingResult bindingResult, @RequestParam Shop shop,
                                @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if (bindingResult.hasErrors()) {
            return getProducts(model, currentUser);
        }
        product.setShop(shop);
        productRepository.save(product);
        return "redirect:/products";
    }
}
