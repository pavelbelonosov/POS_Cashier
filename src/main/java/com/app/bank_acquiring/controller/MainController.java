package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.ProductCart;
import com.app.bank_acquiring.service.ShopService;
import com.app.bank_acquiring.service.ProductService;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductCart productCart;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UposService uposService;


    @GetMapping("/main")
    public String view(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.findByTid(user.getWorkTerminalTid());
        List<Product> products = new ArrayList<>();
        List<Integer> productAmounts = new ArrayList<>();
        productCart.getProducts().forEach((k, v) -> {
            if (k.getBalance() < v) {
                products.add(k);
                productAmounts.add(v);
            }

        });
        model.addAttribute("account", user);
        model.addAttribute("terminal", terminal);
        model.addAttribute("productsInCheque", products);
        model.addAttribute("productsAmountsInCheque", productAmounts);
        return "main";
    }

    @Transactional
    @PostMapping("/shops/{shopId}/products/addtocart")
    public String copyProducts(@RequestParam(name = "prods", required = false) long[] prods,
                               @RequestParam(name = "quantity", required = false) List<Integer> quantity,
                               @PathVariable Long shopId,
                               @AuthenticationPrincipal UserDetails currentUser) {
        if (prods == null || quantity == null) {
            return "redirect:/main";
        }
        List<Integer> quantitylist = quantity.stream().filter(b -> b != null).collect(Collectors.toList());
        shopService.getShop(shopId, currentUser); //just validation issue
        for (int i = 0; i < prods.length; i++) {
            productCart.addToCart(productService.getProduct(prods[i], currentUser), quantitylist.get(i));
        }
        return "redirect:/main";
    }
}
