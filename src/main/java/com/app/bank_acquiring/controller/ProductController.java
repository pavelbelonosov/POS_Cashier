package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.ProductRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.ShopService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.tools.FileObject;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Controller
public class ProductController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ProductService productService;

    @ModelAttribute
    public Product getProduct() {
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
        model.addAttribute("product", productService.getProduct(id, currentUser));
        return "product";
    }

    @GetMapping("/shops/{id}/products/file")
    public ResponseEntity<byte[]> getExcelFile(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Shop shop = shopService.getShop(id, currentUser);
        byte[] content = productService.createExcelFile(user.getId(), shop.getId(), shop.getProducts());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Content-Disposition", "attachment; filename=products_" + shop.getId() + ".xlsx");
        return new ResponseEntity<>(content, headers, HttpStatus.CREATED);
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute Product product, BindingResult bindingResult, @RequestParam Shop shop,
                                @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if (bindingResult.hasErrors()) {
            return getProducts(model, currentUser);
        }
        product.setShop(shop);
        productService.saveProduct(product);
        return "redirect:/products";
    }


    @DeleteMapping("/products/{id}")
    public String deleteProductById(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        productService.deleteProduct(id, currentUser);
        return "redirect:/products";
    }
}
