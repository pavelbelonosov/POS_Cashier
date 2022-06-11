package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.TerminalRepository;
import com.app.bank_acquiring.service.UposService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

@Controller
public class TerminalController {

    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private UposService uposService;
    @Autowired
    private AccountRepository accountRepository;

    /*@GetMapping("/terminals")
    public String list(Model model){
        model.addAttribute("terminals",terminalRepository.findAll());
        return "terminals";
    }*/

    @GetMapping("/terminals")
    public String listAccountTerminals(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        model.addAttribute("account", user);
        return "terminals";
    }

    @GetMapping("/terminals/{id}")
    public String getTerminalById(Model model, @PathVariable Long id,
                              @AuthenticationPrincipal UserDetails currentUser){
        Account user = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(id);
        if(user.getTerminals().contains(terminal)){
            model.addAttribute("term",terminal);
        } else {
            throw new RuntimeException("Current account doesn't have this terminal");
        }
        return "terminal";
    }

   @ModelAttribute
    private Terminal getTerminal() {
        return new Terminal();
    }


    @Transactional
    @PostMapping("/terminals")
    public String setAccountTerminals(@Valid @ModelAttribute Terminal terminal, BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails currentUser, Model model) throws IOException {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", acc);
            return "terminals";
        }
        terminal.setAccount(acc);
        terminalRepository.save(terminal);
        acc.getTerminals().add(terminal);
        uposService.createUserUpos(acc.getId(), terminal);
        return "redirect:/terminals";
    }


    @PostMapping("/terminals/workingterminal")
    public String setAccountWorkTerminal(@RequestParam Long terminalId,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        acc.setWorkTerminalTid(terminalRepository.getOne(terminalId).getTid());
        accountRepository.save(acc);
        return "redirect:/terminals";
    }


    @PostMapping("/terminals/{id}")
    public String updateTerminal(@RequestParam String ip, @RequestParam String chequeHeader,
                                 @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) throws FileNotFoundException {
        Account acc = accountRepository.findByUsername(currentUser.getUsername());
        Terminal terminal = terminalRepository.getOne(id);
        if(!acc.getTerminals().contains(terminal)){
            throw new RuntimeException("Current account doesn't have this terminal");
        }
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
        if(!acc.getTerminals().contains(terminal)){
            throw new RuntimeException("Current account doesn't have this terminal");
        }
        uposService.deleteUserUpos(acc.getId(),terminal.getTid());
        terminalRepository.delete(terminal);
        return "redirect:/terminals";
    }
}
