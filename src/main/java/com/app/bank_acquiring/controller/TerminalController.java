package com.app.bank_acquiring.controller;

import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.service.AccountService;
import com.app.bank_acquiring.service.IdValidationException;
import com.app.bank_acquiring.service.TerminalService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

@Controller
@AllArgsConstructor
public class TerminalController {

    private AccountService accountService;
    private TerminalService terminalService;

    @ModelAttribute
    private Terminal getTerminal() {
        return new Terminal();
    }


    @ExceptionHandler(IdValidationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String exceptionHandler(Model model, IdValidationException ex) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Нет доступа");
        model.addAttribute("message", ex.getMessage());
        return "error";
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
        Terminal terminal = terminalService.getValidatedTerminal(id, currentUser.getUsername());
        Collections.reverse(terminal.getTransactions());
        model.addAttribute("terminal", terminal);
        model.addAttribute("connection", List.of(false, true));//standalone vs integrated pos type
        model.addAttribute("workAccounts",
                terminalService.getEmployeesWithThisWorkTerminal(terminal.getTid(), currentUser.getUsername()));
        return "terminal";
    }

    @GetMapping("/terminals/{id}/test")
    public String testTerminalConnection(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.testConnection(id, currentUser.getUsername());
        return "redirect:/terminals/{id}";
    }

    @PostMapping("/terminals")
    public String setTerminalToCurrentAccount(@Valid @ModelAttribute Terminal terminal, BindingResult bindingResult,
                                              @AuthenticationPrincipal UserDetails currentUser, Model model) {
        if(terminalService.getTerminalByTid(terminal.getTid())!=null){
            bindingResult.addError(new FieldError("terminal", "tid",
                    "Терминал с таким TID уже существует"));
        }
        if (bindingResult.hasErrors()) {
            return getTerminals(model, currentUser);
        }
        try {
            terminalService.addTerminalToAccount(terminal, currentUser.getUsername());
            return "redirect:/terminals";
        } catch (RuntimeException e) {
            bindingResult.addError(new FieldError("terminal", "tid", "Не удалось добавить терминал. Ошибка файловой системы"));
            return getTerminals(model, currentUser);
        }
    }

    @PostMapping("/accounts/current/terminals/workingterminal")
    public String setWorkTerminalToAccount(@RequestParam Long terminalId,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.setWorkTerminalToAccount(currentUser.getUsername(), terminalId);
        return "redirect:/main";
    }


    @PostMapping("/terminals/{id}")
    public String updateTerminal(@RequestParam boolean connection, @RequestParam(required = false) String ip,
                                 @RequestParam(required = false) String chequeHeader, @PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails currentUser) throws FileNotFoundException {
        terminalService.updateTerminal(id, currentUser.getUsername(), connection, ip, chequeHeader);
        return "redirect:/terminals/" + id;
    }

    @DeleteMapping("/terminals/{id}")
    public String deleteTerminal(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        terminalService.deleteTerminal(id, currentUser.getUsername());
        return "redirect:/terminals";
    }
}
