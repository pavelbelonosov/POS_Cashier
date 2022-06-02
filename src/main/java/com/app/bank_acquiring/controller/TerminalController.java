package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.UposService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;

@Controller
public class TerminalController {

    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private UposService uposService;
    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/terminals")
    public String list(Model model){
        model.addAttribute("terminals",terminalRepository.findAll());
        return "terminals";
    }

    @GetMapping("/terminals/{id}")
    public String getTerminal(Model model, @PathVariable Long id){
        model.addAttribute("terminal",terminalRepository.getOne(id));
        return "terminal";
    }

    @PostMapping("/terminals")
    public String add(@ModelAttribute Terminal terminal){
        terminalRepository.save(terminal);
        return "redirect:/terminals";
    }

    @PostMapping("/terminals/{id}")
    public String updateTerminal(@RequestParam String ip, @RequestParam String chequeHeader,
                                 @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) throws FileNotFoundException {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(id);
        if(!ip.isEmpty()){
            terminal.setIp(ip);
        }
        if(!chequeHeader.isEmpty()){
            terminal.setChequeHeader(chequeHeader);
        }
        terminalRepository.save(terminal);
        uposService.updateUposSettings(acc.getId(),terminal);
        return "redirect:/terminals/"+id;
    }

    @DeleteMapping("/terminals/{id}")
    public String deleteTerminal(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(id);
        uposService.deleteUserUpos(acc.getId(),terminal.getTid());
        terminalRepository.delete(terminal);
        return "redirect:/accounts/current";
    }
}
