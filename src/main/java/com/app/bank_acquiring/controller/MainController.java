package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.service.ProductCartComponent;
import com.app.bank_acquiring.service.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class MainController {

    private AccountService accountService;
    private ShopService shopService;
    private ProductService productService;
    private TerminalService terminalService;
    private ProductCartComponent productCart;


    @GetMapping("/main")
    public String getCashierView(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalService.getTerminalByTid(user.getWorkTerminalTid());
        List<Product> products = new ArrayList<>();
        List<Double> productAmounts = new ArrayList<>();
        productCart.getProductsWithAmount().forEach((k, v) -> {
            products.add(k);
            productAmounts.add(v);
        });
        model.addAttribute("account", user);
        model.addAttribute("terminal", terminal);
        model.addAttribute("productsInCheque", products);
        model.addAttribute("productsAmountsInCheque", productAmounts);
        model.addAttribute("chequeTotalPrice", productCart.getTotalPrice());
        return "main";
    }

    @Transactional
    @PostMapping("/shops/{shopId}/products/addtocart")
    public String addProductsInCart(@RequestParam(name = "prods", required = false) long[] prods,
                                    @RequestParam(name = "quantity", required = false) List<Double> quantity,
                                    @PathVariable Long shopId,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        if (prods == null || quantity == null) {
            return "redirect:/main";
        }
        List<Double> quantitylist = quantity.stream().filter(b -> b != null).collect(Collectors.toList());
        if (prods.length != quantitylist.size()) {
            return "redirect:/main";
        }
        shopService.getShop(shopId, currentUser.getUsername()); //just validation issue
        for (int i = 0; i < prods.length; i++) {
            productCart.addToCart(productService.getProduct(prods[i], currentUser.getUsername()), quantitylist.get(i));
        }
        return "redirect:/main";
    }


    @GetMapping("/shops/{shopId}/productcart/{productId}/amount/{productAmount}")
    public String deleteProductFromCart(@PathVariable Long shopId, @PathVariable Long productId,
                                        @PathVariable Double productAmount,
                                        @AuthenticationPrincipal UserDetails currentUser) {

        shopService.getShop(shopId, currentUser.getUsername()); //just validation issue
        Product product = productService.getProduct(productId, currentUser.getUsername());
        if (product.getMeasurementUnit() == MeasurementUnit.LITER || product.getMeasurementUnit() == MeasurementUnit.KILOGRAM) {
            productCart.addToCart(product, -productAmount);
        } else {
            productCart.addToCart(product, -1);
        }
        return "redirect:/main";
    }


    @GetMapping("/shops/{shopId}/productcart/delete")
    public String clearCart(@PathVariable Long shopId, @AuthenticationPrincipal UserDetails currentUser) {
        shopService.getShop(shopId, currentUser.getUsername()); //just validation issue
        productCart.getProductsWithAmount().clear();
        return "redirect:/main";
    }
}
