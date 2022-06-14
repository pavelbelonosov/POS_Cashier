package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
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

    @GetMapping("/main")
    public String view(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.findByTid(user.getWorkTerminalTid());
        model.addAttribute("account", user);
        model.addAttribute("terminal",terminal);
        return "main";
    }
}
