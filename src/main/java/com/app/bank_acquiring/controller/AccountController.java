package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;

import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    //@Autowired
    //UserDetailsService userDetailsService;

    /*@PostConstruct
    public void init() {
        Account user1 = new Account();
        user1.setUsername("user");
        user1.setPassword(passwordEncoder.encode("qwerty"));
        user1.setAuthority("admin");



        accountRepository.save(user1);
    }*/

    @GetMapping("/accounts")
    public String view(Model model) {
        model.addAttribute("accounts", accountRepository.findAll());
        return "accounts";
    }

    @GetMapping("/accounts/current")
    public String viewAccount(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        model.addAttribute("account", user);
        model.addAttribute("terminals", user.getTerminals());
        return "account";
    }

    @Transactional
    @PostMapping("/accounts/current/terminals")
    public String setAccountTerminals(@RequestParam int tid, @RequestParam String ip, @RequestParam String chequeHeader,
                                      @AuthenticationPrincipal UserDetails currentUser) throws IOException {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = new Terminal();
        terminal.setTid(tid);
        terminal.setIp(ip);
        terminal.setChequeHeader(chequeHeader);
        terminal.setAccount(acc);
        terminalRepository.save(terminal);
        acc.getTerminals().add(terminal);
        uposService.createUserUpos(acc.getId(), terminal);
        return "redirect:/accounts/current";
    }

    @PostMapping("/accounts/current/terminals/workingterminal")
    public String setAccountWorkTerminal(@RequestParam Long terminalId,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        acc.setWorkTerminalTid(terminalRepository.getOne(terminalId).getTid());
        accountRepository.save(acc);
        return "redirect:/accounts/current";
    }

    @PostMapping("/accounts")
    public String create(@RequestParam String username,
                         @RequestParam String password) {
        if (accountRepository.findByUsername(username) != null) {
            return "redirect:/accounts";
        }
        Account acc = new Account();
        acc.setUsername(username);
        acc.setPassword(passwordEncoder.encode(password));
        acc.setAuthority("cashier");
        accountRepository.save(acc);
        return "redirect:/login";
    }

}
