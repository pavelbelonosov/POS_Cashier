package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;

import com.app.bank_acquiring.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.persistence.PreRemove;
import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ShopRepository shopRepository;

    private List<String> errors = new ArrayList<>();


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
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        /*List<List<Account>> employees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> accs = employees.stream().flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());*/

        model.addAttribute("employees", getEmployees(owner));
        model.addAttribute("owner", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "accounts";
    }


    @GetMapping("/accounts/{id}")
    public String getAccountById(Model model, @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        Account employee = accountRepository.getOne(id);
        /*List<List<Account>> employeeslists = owner.getShops().stream()
                .map(ownershop -> ownershop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = employeeslists.stream().flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());
        if (!employees.contains(employee)) {
            throw new RuntimeException("Current account doesn't have access to this user");
        }*/
        validateIdAccess(id, owner);
        model.addAttribute("employee", employee);
        model.addAttribute("owner", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        model.addAttribute("errors", errors);
        return "account";
    }

    @GetMapping("/accounts/registration")
    public String getRegistrationForm(Model model) {
        //model.addAttribute("errors", errors);
        return "registration";
    }


    @GetMapping("/accounts/current")
    public String getCurrentAccount(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        model.addAttribute("employee", user);
        model.addAttribute("owner", user);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        model.addAttribute("errors", errors);
        return "account";
    }


    /*@PostMapping("/accounts/current/terminals/workingterminal")
    public String setAccountWorkTerminal(@RequestParam Long terminalId,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        acc.setWorkTerminalTid(terminalRepository.getOne(terminalId).getTid());
        accountRepository.save(acc);
        return "redirect:/accounts/current";
    }*/


    @Transactional
    @PostMapping("/accounts")
    public String createEmployee(@Valid @ModelAttribute Account account, BindingResult bindingResultAccount,
                                 @Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                 @RequestParam Shop shop, @AuthenticationPrincipal UserDetails currentUser, Model model) {

        if (bindingResultAccount.hasErrors() || bindingResultAccountInfo.hasErrors()) {
            return getAccounts(model, currentUser);
        }
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountInfo.setAccount(account);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
        shopRepository.getOne(shop.getId()).getAccounts().add(account);
        return "redirect:/accounts";
    }

    @Transactional
    @PostMapping("/accounts/registration")
    public String createAdminUser(@Valid @ModelAttribute Account account, BindingResult bindingResultAccount,
                                  @Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                  @RequestParam String repeatPWD, Model model) {

        if (accountRepository.findByUsername(account.getUsername()) != null) {
            FieldError fieldError = new FieldError("account", "username", "Логин занят");
            bindingResultAccount.addError(fieldError);
        }
        if (!account.getPassword().equals(repeatPWD)) {
            FieldError fieldError = new FieldError("account", "password", "Пароли не совпадают");
            bindingResultAccount.addError(fieldError);
        }
        if (bindingResultAccount.hasErrors() || bindingResultAccountInfo.hasErrors()) {
            return getRegistrationForm(model);
        }
        accountInfo.setAccount(account);
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setAuthority(Authority.ADMIN);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);

        return "redirect:/login";
    }

    @Transactional
    @PostMapping("/accounts/{id}")
    public String updateAccount(@ModelAttribute Account account, @Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResultAccountInfo,
                                @RequestParam Shop shop, @PathVariable Long id,
                                @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        validateIdAccess(id, current);
        if (bindingResultAccountInfo.hasErrors()) {
            return getAccountById(model, id, currentUser);
        }
        Account user = accountRepository.getOne(id);
        AccountInfo userInfo = user.getAccountInfo();
        Shop oldShop = user.getShops().get(0);

        userInfo.setFirstName(accountInfo.getFirstName());
        userInfo.setLastName(accountInfo.getLastName());
        userInfo.setTelephoneNumber(accountInfo.getTelephoneNumber());
        userInfo.setEmail(accountInfo.getEmail());

        user.setAuthority(account.getAuthority());
        shopRepository.getOne(oldShop.getId()).getAccounts().remove(user);
        shopRepository.getOne(shop.getId()).getAccounts().add(user);

        return "redirect:/accounts/" + id;
    }

    @PostMapping("/accounts/current")
    public String updateCurrentAccount(@Valid @ModelAttribute AccountInfo accountInfo, BindingResult bindingResult,
                                       @AuthenticationPrincipal UserDetails currentUser, Model model) {
        Account current = accountRepository.findByUsername(currentUser.getUsername());
        AccountInfo userInfo = current.getAccountInfo();

        if (bindingResult.hasErrors()) {
            return getCurrentAccount(model, currentUser);
        }
        userInfo.setFirstName(accountInfo.getFirstName());
        userInfo.setLastName(accountInfo.getLastName());
        userInfo.setTelephoneNumber(accountInfo.getTelephoneNumber());
        userInfo.setEmail(accountInfo.getEmail());

        accountInfoRepository.save(userInfo);
        return "redirect:/accounts/current";
    }

    @PostMapping("/accounts/current/newpwd")
    public String changePwdCurrentUser(@RequestParam String oldPassword, @RequestParam String password, @RequestParam String repeatPwd,
                                       @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        errors.clear();
        if (password.length() >= 8 && passwordEncoder.matches(oldPassword, owner.getPassword())
                && password.equals(repeatPwd)) {
            owner.setPassword(passwordEncoder.encode(password));
            accountRepository.save(owner);
            errors.add("Успешно");
        } else if (!passwordEncoder.matches(oldPassword, owner.getPassword())) {
            errors.add("Неверный пароль");
        } else if (!password.equals(repeatPwd)) {
            errors.add("Пароли не сопадают");
        }
        return "redirect:/accounts/current";

    }

    @PostMapping("/accounts/{id}/newpwd")
    public String changePwd(@PathVariable Long id, @RequestParam String password, @RequestParam String repeatPwd,
                            @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        validateIdAccess(id, owner);
        Account employee = accountRepository.getOne(id);
        errors.clear();
        if (password.length() >= 8 && password.equals(repeatPwd)) {
            employee.setPassword(passwordEncoder.encode(password));
            accountRepository.save(employee);
            errors.add("Успешно");
        } else {
            errors.add("Пароли не совпадают");
        }
        return "redirect:/accounts/" + id;

    }

    private List<Account> getEmployees(Account owner) {
        List<List<Account>> listOfListsEmployees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = listOfListsEmployees.stream()
                .flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());
        return employees;
    }

    private void validateIdAccess(Long id, Account owner) {
        Account employee = accountRepository.getOne(id);
        if (!getEmployees(owner).contains(employee)) {
            throw new RuntimeException("Current account doesn't have access to this user");
        }
    }

/*
    @DeleteMapping("/accounts/{id}")
    public String deleteTerminal(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        Account user = accountRepository.getOne(id);
        /*if(!acc.getTerminals().contains(terminal)){
            throw new RuntimeException("Current account doesn't have this terminal");
        }
        //shopRepository.getOne(user.)
        accountRepository.delete(user);
        return "redirect:/accounts";
    }*/

}
