package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;

import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.ShopService;
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
import java.util.ListIterator;

@Controller
public class ShopController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private ShopService shopService;

    @ModelAttribute
    public Shop getNewShop() {
        return new Shop();
    }

    @GetMapping("/shops")
    public String getShops(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("shops", user.getShops());
        return "shops";
    }

    @PostMapping("/shops")
    public String createShop(@Valid @ModelAttribute Shop shop, BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if (bindingResult.hasErrors()) {
           return getShops(model, currentUser);
        }
        shopService.createShop(shop,currentUser);
        return "redirect:/shops";
    }

    @DeleteMapping("/shops/{id}")
    public String deleteShop(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails currentUser) {
        shopService.deleteShop(id,currentUser);
        return "redirect:/shops";
    }


    @DeleteMapping("/shops/{shopId}/accounts/{accountId}")
    public String deleteAccount(@PathVariable Long shopId, @PathVariable Long accountId,
                                @AuthenticationPrincipal UserDetails currentUser) {
        shopService.deleteAccountFromShop(shopId,accountId,currentUser);
        return "redirect:/accounts";
    }
}
