package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;

import com.app.bank_acquiring.repository.ShopRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.UposService;
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
    private TerminalRepository terminalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UposService uposService;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ShopRepository shopRepository;
    private List<String> errors = new ArrayList<>();


    /*@PostConstruct
    public void init() {
        Account user1 = new Account();
        user1.setUsername("user");
        user1.setPassword(passwordEncoder.encode("qwerty"));
        user1.setAuthority("admin");



        accountRepository.save(user1);
    }*/
    @Secured("ADMIN")
    @GetMapping("/accounts")
    public String view(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        List<List<Account>> employees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> accs = employees.stream().flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());

        model.addAttribute("employees", accs);
        model.addAttribute("account", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "accounts";
    }

    @Secured("ADMIN")
    @GetMapping("/accounts/{id}")
    public String getAccountById(Model model, @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account owner = accountRepository.findByUsername(currentUser.getUsername());
        Account employee = accountRepository.getOne(id);
        List<List<Account>> employeeslists = owner.getShops().stream()
                .map(ownershop -> ownershop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = employeeslists.stream().flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());
        if (!employees.contains(employee)) {
            throw new RuntimeException("Current account doesn't have access to this user");
        }
        model.addAttribute("employee", employee);
        model.addAttribute("owner", owner);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        return "account";
    }

    @GetMapping("/accounts/registration")
    public String viewRegistrationForm(Model model) {
        model.addAttribute("errors", errors);
        return "registration";
    }


    @GetMapping("/accounts/current")
    public String viewAccount(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        model.addAttribute("employee", user);
        model.addAttribute("owner", user);
        model.addAttribute("authorities", Arrays.stream(Authority.values())
                .filter(authority -> !authority.toString().equals("ADMIN")).toArray());
        //model.addAttribute("account", user);
        //model.addAttribute("terminals", user.getTerminals());
        //model.addAttribute("terminal", new Terminal());
        return "account";
    }

    @ModelAttribute
    private Terminal getTerminal() {
        return new Terminal();
    }

    /*@Transactional
    @PostMapping("/accounts/current/terminals")
    public String setAccountTerminals(@Valid @ModelAttribute Terminal terminal, BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails currentUser, Model model) throws IOException {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", acc);
            model.addAttribute("terminals", acc.getTerminals());
            return "account";
        }

        //Terminal terminal = new Terminal();
        //terminal.setTid(tid);
        //terminal.setMid(mid);
        //terminal.setIp(ip);
        //terminal.setChequeHeader(chequeHeader);
        terminal.setAccount(acc);
        terminalRepository.save(terminal);
        acc.getTerminals().add(terminal);
        uposService.createUserUpos(acc.getId(), terminal);
        return "redirect:/accounts/current";
    }*/

    /*@PostMapping("/accounts/current/terminals/workingterminal")
    public String setAccountWorkTerminal(@RequestParam Long terminalId,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        acc.setWorkTerminalTid(terminalRepository.getOne(terminalId).getTid());
        accountRepository.save(acc);
        return "redirect:/accounts/current";
    }*/
    @Secured("ADMIN")
    @Transactional
    @PostMapping("/accounts")
    public String createEmployee(@ModelAttribute Account account, @ModelAttribute AccountInfo accountInfo,
                                 @RequestParam Shop shop, @AuthenticationPrincipal UserDetails currentUser) {
        /*if(shop==null){
            return "redirect:/accounts";
        }
        Account acc = new Account();
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setFirstName(firstName);
        accountInfo.setLastName(lastName);
        accountInfo.setAccount(acc);

        acc.setPassword(passwordEncoder.encode(password));
        acc.setAuthority(authority);

        accountInfoRepository.save(accountInfo);
        accountRepository.save(acc);
        shopRepository.getOne(shop.getId()).getAccounts().add(acc);
        shopRepository.save(shop);*/
        accountInfo.setAccount(account);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
        shopRepository.getOne(shop.getId()).getAccounts().add(account);
        shopRepository.save(shop);

        return "redirect:/accounts";
    }

    @PostMapping("/accounts/registration")
    public String createAdminUser(@RequestParam String username,
                                  @RequestParam String password) {
        if (accountRepository.findByUsername(username) != null) {
            errors.add("Логин занят");
            return "redirect:/accounts/registration";
        }
        Account acc = new Account();
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccount(acc);
        acc.setUsername(username);
        acc.setPassword(passwordEncoder.encode(password));
        acc.setAuthority(Authority.ADMIN);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(acc);
        return "redirect:/login";
    }

    @Secured("ADMIN")
    @PostMapping("/accounts/{id}")
    public String updateAccount(@ModelAttribute Account account, @ModelAttribute AccountInfo accountInfo,
                                @RequestParam Shop shop, @PathVariable Long id,
                                @AuthenticationPrincipal UserDetails currentUser) {
        //Account owner = accountRepository.findByUsername(currentUser.getUsername());
        Account user = accountRepository.getOne(id);
        AccountInfo userInfo = user.getAccountInfo();
        Shop oldShop = user.getShops().get(0);

        userInfo.setFirstName(accountInfo.getFirstName());
        userInfo.setLastName(accountInfo.getLastName());
        userInfo.setTelephoneNumber(accountInfo.getTelephoneNumber());
        userInfo.setEmail(accountInfo.getEmail());
        user.setPassword(passwordEncoder.encode(account.getPassword()));
        user.setAuthority(account.getAuthority());

        accountInfoRepository.save(userInfo);
        accountRepository.save(user);
        shopRepository.getOne(oldShop.getId()).getAccounts().remove(user);
        shopRepository.getOne(shop.getId()).getAccounts().add(user);
        shopRepository.save(shop);
        return "redirect:/accounts/" + id;
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
