package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;


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
        model.addAttribute("product", productService.getProduct(id, currentUser.getUsername()));
        return "product";
    }

    @GetMapping("/shops/{id}/products/file")
    public ResponseEntity<byte[]> getExcelFile(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Shop shop = shopService.getShop(id, currentUser.getUsername());
        byte[] content = productService.createExcelFile(user.getId(), shop.getId(), shop.getProducts());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Content-Disposition", "attachment; filename=products_" + shop.getId() + ".xlsx");
        return new ResponseEntity<>(content, headers, HttpStatus.CREATED);
    }

    @GetMapping("/products/refresh")
    public String refreshProducts(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("account", user);
        return "products::#prodTable";
    }

    @Transactional
    @PostMapping("/products/updateBalance")
    @CacheEvict(value = "products", allEntries = true)
    public String updateBalance(@RequestParam(name = "prods", required = false) long[] prods,
                                @RequestParam(name = "balances", required = false) List<String> balances,
                                @AuthenticationPrincipal UserDetails currentUser) {
        if (prods == null || balances == null) {
            return "redirect:/products";
        }
        List<String> list = balances.stream().filter(b -> !b.isEmpty()).collect(Collectors.toList());
        if (prods.length == list.size()) {
            for (int i = 0; i < prods.length; i++) {
                productService.getProduct(prods[i], currentUser.getUsername()).setBalance(Double.parseDouble(list.get(i)));
            }
        }
        return "redirect:/products";
    }

    @Transactional
    @PostMapping("/shops/{shopId}/products/copy")
    public String copyProducts(@RequestParam(name = "prods", required = false) long[] prods,
                               @PathVariable Long shopId, @RequestParam(required = false) Long targetShopId,
                               @AuthenticationPrincipal UserDetails currentUser) {
        if (prods == null || targetShopId == null) {
            return "redirect:/products";
        }
        productService.copyProducts(prods, shopId, targetShopId, currentUser.getUsername());
        return "redirect:/products";
    }

    @PostMapping("/products")
    @CacheEvict(value = "products", allEntries = true)
    public String createProduct(@Valid @ModelAttribute Product product, BindingResult bindingResult, @RequestParam Shop shop,
                                @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if (shop == null) {
            bindingResult.addError(new FieldError("product", "shop", "Укажите магазин"));
        }
        if (bindingResult.hasErrors()) {
            return getProducts(model, currentUser);
        }
        if (product.getType() == Type.SERVICE) {
            product.setBalance(Integer.MAX_VALUE);
            product.setMeasurementUnit(MeasurementUnit.UNIT);
        }
        product.setShop(shop);
        productService.saveProduct(product);
        return "redirect:/products";
    }


    @DeleteMapping("/products/{id}")
    @CacheEvict(value = "products", key = "#id"+"#currentUser")
    public String deleteProductById(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        productService.deleteProduct(id, currentUser.getUsername());
        return "redirect:/products";
    }

    @PostMapping("/products/deleteMany")
    @CacheEvict(value = "products", allEntries = true)
    public String deleteMany(@RequestParam(required = false) long[] prods, @AuthenticationPrincipal UserDetails currentUser) {
        if (prods != null) {
            for (int i = 0; i < prods.length; i++) {
                productService.deleteProduct(prods[i], currentUser.getUsername());
            }
        }
        return "redirect:/products";
    }
}
