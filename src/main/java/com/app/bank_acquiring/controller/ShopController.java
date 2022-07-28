package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;

import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.ShopService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@AllArgsConstructor
public class ShopController {

    private AccountService accountService;
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
        shopService.bundleShopWithAccount(shop, currentUser.getUsername());
        return "redirect:/shops";
    }

    @DeleteMapping("/shops/{id}")
    public String deleteShop(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails currentUser) {
        shopService.deleteShop(id, currentUser.getUsername());
        return "redirect:/shops";
    }


    @DeleteMapping("/shops/{shopId}/accounts/{accountId}")
    public String deleteAccount(@PathVariable Long shopId, @PathVariable Long accountId,
                                @AuthenticationPrincipal UserDetails currentUser) {
        shopService.deleteAccountFromShop(shopId, accountId, currentUser.getUsername());
        return "redirect:/accounts";
    }
}
