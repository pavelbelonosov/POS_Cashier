package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.TerminalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

@Controller
public class TerminalController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private TerminalService terminalService;

    /*@GetMapping("/terminals")
    public String list(Model model){
        model.addAttribute("terminals",terminalRepository.findAll());
        return "terminals";
    }*/

    @ModelAttribute
    private Terminal getTerminal() {
        return new Terminal();
    }

    @GetMapping("/terminals")
    public String getTerminals(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Account user = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("account", user);
        return "terminals";
    }

    @GetMapping("/terminals/{id}")
    public String getTerminalById(Model model, @PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        Terminal terminal = terminalService.getValidatedTerminal(id, currentUser);
        Collections.reverse(terminal.getTransactions());
        model.addAttribute("terminal",terminal);
        model.addAttribute("workAccounts", terminalService.getAccountsWithWorkTerminal(terminal,currentUser));
        return "terminal";
    }

    @PostMapping("/terminals/{id}/test")
    public String testTerminalConnection(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.testConnection(id,currentUser);
        return "redirect:/terminals/{id}";
    }

    @PostMapping("/terminals")
    public String setTerminalToCurrentAccount(@Valid @ModelAttribute Terminal terminal, BindingResult bindingResult,
                                              @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if (bindingResult.hasErrors()) {
            return getTerminals(model, currentUser);
        }
        try {
            terminalService.addTerminalToAccount(terminal, currentUser);
            return "redirect:/terminals";
        } catch (IOException e) {
            bindingResult.addError(new FieldError("terminal", "tid", "Не удалось добавить терминал. Ошибка файловой ситсемы"));
            return getTerminals(model, currentUser);
        }
    }

    @PostMapping("/accounts/current/terminals/workingterminal")
    public String setWorkTerminalToAccount(@RequestParam Long terminalId,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.setWorkTerminalToAccount(currentUser, terminalId);
        return "redirect:/main";
    }


    @PostMapping("/terminals/{id}")
    public String updateTerminal(@RequestParam String ip, @RequestParam String chequeHeader,
                                 @PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) throws FileNotFoundException {
        terminalService.updateTerminal(id, currentUser, ip, chequeHeader);
        return "redirect:/terminals/" + id;
    }

    @DeleteMapping("/terminals/{id}")
    public String deleteTerminal(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.deleteTerminal(id, currentUser);
        return "redirect:/terminals";
    }
}
