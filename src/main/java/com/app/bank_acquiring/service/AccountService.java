package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AccountService {
    private final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private AccountRepository accountRepository;
    private PasswordEncoder passwordEncoder;
    private AccountInfoRepository accountInfoRepository;
    private ShopRepository shopRepository;

    @Transactional
    public void createAdminUser(@NonNull Account account, @NonNull AccountInfo accountInfo) {
        accountInfo.setAccount(account);
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setAuthority(Authority.ADMIN);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
    }

    @Transactional
    public void createEmployee(@NonNull Account account, @NonNull AccountInfo accountInfo, @NonNull Shop shop) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountInfo.setAccount(account);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
        shopRepository.getOne(shop.getId()).getAccounts().add(account);
    }

    @Transactional
    public void updateEmployeeAccount(@NonNull Long id, @NonNull AccountInfo accountInfo,
                                      @NonNull Shop shop, @NonNull Authority authority) {
        Account user = accountRepository.getOne(id);
        AccountInfo userInfo = user.getAccountInfo();
        Shop oldShop = user.getShops().get(0);

        userInfo.setFirstName(accountInfo.getFirstName());
        userInfo.setLastName(accountInfo.getLastName());
        userInfo.setTelephoneNumber(accountInfo.getTelephoneNumber());
        userInfo.setEmail(accountInfo.getEmail());

        user.setAuthority(authority);
        shopRepository.getOne(oldShop.getId()).getAccounts().remove(user);
        shopRepository.getOne(shop.getId()).getAccounts().add(user);
    }

    @Transactional
    public void updateCurrentAccount(Account current, AccountInfo newAccountInfo) {
        AccountInfo userInfo = current.getAccountInfo();
        if (newAccountInfo.getFirstName() != null && !newAccountInfo.getFirstName().isEmpty()) {
            userInfo.setFirstName(newAccountInfo.getFirstName());
        }
        if (newAccountInfo.getLastName() != null && !newAccountInfo.getLastName().isEmpty()) {
            userInfo.setLastName(newAccountInfo.getLastName());
        }
        if (newAccountInfo.getTelephoneNumber() != null && !newAccountInfo.getTelephoneNumber().isEmpty()) {
            userInfo.setTelephoneNumber(newAccountInfo.getTelephoneNumber());
        }
        if (newAccountInfo.getEmail() != null && !newAccountInfo.getEmail().isEmpty()) {
            userInfo.setEmail(newAccountInfo.getEmail());
        }
        accountInfoRepository.save(userInfo);
    }

    public void changeCurrentAccountPassword(Account account, String newPassword) {
        if (account != null && newPassword != null && !newPassword.isEmpty()) {
            account.setPassword(passwordEncoder.encode(newPassword));
            accountRepository.save(account);
        }
    }

    @Transactional
    public void changeEmployeePassword(Long id, String newPassword) {
        if (id != null && newPassword != null && !newPassword.isEmpty()) {
            Account employee = accountRepository.findById(id).orElse(null);
            if (employee != null) {
                employee.setPassword(passwordEncoder.encode(newPassword));
            }
        }
    }

    public Account findByUsername(String username) {
        if (username != null && !username.isEmpty() && !username.isBlank()) {
            return accountRepository.findByUsername(username);
        }
        return null;
    }

    public Account getAccountById(Long id) {
        return id != null ? accountRepository.getOne(id) : null;
    }

    public List<Account> getEmployees(Account owner) {
        if (owner != null) {
            List<List<Account>> listOfListsEmployees = owner.getShops().stream()
                    .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
            List<Account> employees = listOfListsEmployees.stream()
                    .flatMap(List::stream).filter(acc -> !acc.equals(owner))
                    .collect(Collectors.toList());
            return employees;
        }
        return new ArrayList<>();
    }

    public void validateIdAccess(Long id, Account owner) {
        if (id == null || owner == null) {
            logger.error("ID validation error: " + (id == null ? "given ID is null" : "")
                    + (owner == null ? "given account is null" : ""));
            throw new IdValidationException("Invalid user id or current account");
        }
        if (id.equals(owner.getId())) {
            return;
        }
        Account employee = accountRepository.findById(id).orElse(null);
        if (employee == null || owner.getAuthority() != Authority.ADMIN
                || !getEmployees(owner).contains(employee)) {
            logger.error("ID validation error: given owner account(id "
                    + owner.getId() + ") doesn't have permission to this employee(id "
                    + (employee != null ? employee.getId() : id + " not valid") + ")");
            throw new IdValidationException("Current account doesn't have access to this user");
        }
    }

}
