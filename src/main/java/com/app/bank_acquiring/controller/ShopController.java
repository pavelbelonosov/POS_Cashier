package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.repository.AccountRepository;

import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ShopController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ShopRepository shopRepository;

    @Secured("ADMIN")
    @GetMapping("/shops")
    public String listShops(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        model.addAttribute("shops", user.getShops());
        return "shops";
    }

    @ModelAttribute
    public Shop getNewShop() {
        return new Shop();
    }

    @Secured("ADMIN")
    @PostMapping("/shops")
    public String createShop(@Valid @ModelAttribute Shop shop, BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("shops", acc.getShops());
            return "shops";
        }
        List<Account> list = new ArrayList<>();
        list.add(acc);
        shop.setAccounts(list);
        shopRepository.save(shop);

        return "redirect:/shops";
    }
    @Secured("ADMIN")
    @Transactional
    @DeleteMapping("/shops/{shopId}/accounts/{accountId}")
    public String deleteAccount(@PathVariable Long shopId, @PathVariable Long accountId,
                                @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        Account employee = accountRepository.getOne(accountId);
        System.out.println(employee.getUsername());
        Shop shop = shopRepository.getOne(shopId);
        if (!owner.getShops().contains(shop)) {
            throw new RuntimeException("Current account doesn't have this shop");
        }
        shop.getAccounts().remove(employee);
        accountRepository.delete(employee);
        return "redirect:/accounts";
    }
}
