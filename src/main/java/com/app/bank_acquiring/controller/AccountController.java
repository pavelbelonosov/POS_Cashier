package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;

@Controller
public class AccountController {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountService accountService;

    @ModelAttribute
    private Account getAccount() {
        return new Account();
    }

    @ModelAttribute
    private AccountInfo getAccountInfo() {
        return new AccountInfo();
    }

    @GetMapping("/accounts")
    public String getAccounts(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("employees", accountService.getEmployees(owner));
        model.addAttribute("owner", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "accounts";
    }

    @GetMapping("/accounts/{id}")
    public String getAccount(Model model, @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountService.findByUsername(currentUser.getUsername());
        Account employee = accountService.getAccountById(id);
        accountService.validateIdAccess(id, owner);
        model.addAttribute("employee", employee);
        model.addAttribute("owner", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "account";
    }

    @GetMapping("/accounts/current")
    public String getCurrentAccount(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("employee", user);
        model.addAttribute("owner", user);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "account";
    }

    @GetMapping("/accounts/registration")
    public String getRegistrationForm() {
        return "registration";
    }

    @PostMapping("/accounts/registration")
    public String createAdminUser(@Valid @ModelAttribute Account account, BindingResult bindingResultAccount,
                                  @Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                  @RequestParam String repeatPWD) {

        if (accountService.findByUsername(account.getUsername()) != null) {
            FieldError fieldError = new FieldError("account", "username", "Логин занят");
            bindingResultAccount.addError(fieldError);
        }
        if (!account.getPassword().equals(repeatPWD)) {
            FieldError fieldError = new FieldError("account", "password", "Пароли не совпадают");
            bindingResultAccount.addError(fieldError);
        }
        if (bindingResultAccount.hasErrors() || bindingResultAccountInfo.hasErrors()) {
            return getRegistrationForm();
        }
        accountService.createAdminUser(account, accountInfo);
        return "redirect:/login";
    }

    @PostMapping("/accounts")
    public String createEmployee(@Valid @ModelAttribute Account account, BindingResult bindingResultAccount,
                                 @Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                 @RequestParam Shop shop, @AuthenticationPrincipal UserDetails currentUser, Model model) {

        if (accountService.findByUsername(account.getUsername()) != null) {
            FieldError fieldError = new FieldError("account", "username", "Логин занят");
            bindingResultAccount.addError(fieldError);
        }
        if (bindingResultAccount.hasErrors() || bindingResultAccountInfo.hasErrors()) {
            return getAccounts(model, currentUser);
        }
        accountService.createEmployee(account, accountInfo, shop);
        return "redirect:/accounts";
    }

    @PostMapping("/accounts/{id}")
    public String updateAccount(@Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                @RequestParam Shop shop, @RequestParam Authority authority, @PathVariable Long id,
                                @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account current = accountService.findByUsername(currentUser.getUsername());
        accountService.validateIdAccess(id, current);
        if (bindingResultAccountInfo.hasErrors()) {
            return getAccount(model, id, currentUser);
        }
        accountService.updateEmployeeAccount(id, accountInfo, shop, authority);
        return "redirect:/accounts/" + id;
    }

    @PostMapping("/accounts/current")
    public String updateCurrentAccount(@Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResult,
                                       @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account current = accountService.findByUsername(currentUser.getUsername());
        if (bindingResult.hasErrors()) {
            return getCurrentAccount(model, currentUser);
        }
        accountService.updateCurrentAccount(current,accountInfo);
        return "redirect:/accounts/current";
    }

    @PostMapping("/accounts/current/newpwd")
    public String changeCurrentAccountPassword(@Valid @ModelAttribute Account account, BindingResult bindingResult,
                                       @RequestParam String newPassword, @RequestParam String repeatPwd,
                                       @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account owner = accountService.findByUsername(currentUser.getUsername());
        if (!passwordEncoder.matches(account.getPassword(), owner.getPassword())) {
            FieldError fieldError = new FieldError("account", "password", "Неверный пароль");
            bindingResult.addError(fieldError);
        }
        if (!newPassword.equals(repeatPwd)) {
            FieldError fieldError = new FieldError("account", "password", "Пароли не совпадают");
            bindingResult.addError(fieldError);
        }
        if (newPassword.contains(" ") || newPassword.length() < 8) {
            FieldError fieldError = new FieldError("account", "password", "Недопустимый пароль");
            bindingResult.addError(fieldError);
        }
        if (bindingResult.hasFieldErrors("password")) {
            return getCurrentAccount(model, currentUser);
        }
        accountService.changeCurrentAccountPassword(owner,newPassword);
        return "redirect:/accounts/current";

    }

    @PostMapping("/accounts/{id}/newpwd")
    public String changeAccountPassword(@PathVariable Long id, @ModelAttribute Account account, BindingResult bindingResult,
                            @RequestParam String newPassword, @RequestParam String repeatPwd,
                            @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account owner = accountService.findByUsername(currentUser.getUsername());
        accountService.validateIdAccess(id, owner);
        if (!newPassword.equals(repeatPwd)) {
            FieldError fieldError = new FieldError("account", "password", "Пароли не совпадают");
            bindingResult.addError(fieldError);
        }
        if (newPassword.contains(" ") || newPassword.length() < 8) {
            FieldError fieldError = new FieldError("account", "password", "Недопустимый пароль");
            bindingResult.addError(fieldError);
        }
        if (bindingResult.hasFieldErrors("password")) {
            return getAccount(model, id, currentUser);
        }
        accountService.changeEmployeePassword(id,newPassword);
        return "redirect:/accounts/" + id;
    }

}
