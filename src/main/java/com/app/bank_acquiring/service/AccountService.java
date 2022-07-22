package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ShopRepository shopRepository;

    @Transactional
    public void createAdminUser(Account account, AccountInfo accountInfo) {
        accountInfo.setAccount(account);
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setAuthority(Authority.ADMIN);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
    }

    @Transactional
    public void createEmployee(Account account, AccountInfo accountInfo, Shop shop) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountInfo.setAccount(account);
        accountInfoRepository.save(accountInfo);
        accountRepository.save(account);
        shopRepository.getOne(shop.getId()).getAccounts().add(account);
    }

    @Transactional
    public void updateEmployeeAccount(Long id, AccountInfo accountInfo, Shop shop, Authority authority) {
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
        if (!newAccountInfo.getFirstName().isEmpty()) {
            userInfo.setFirstName(newAccountInfo.getFirstName());
        }
        if (!newAccountInfo.getLastName().isEmpty()) {
            userInfo.setLastName(newAccountInfo.getLastName());
        }
        if (!newAccountInfo.getTelephoneNumber().isEmpty()) {
            userInfo.setTelephoneNumber(newAccountInfo.getTelephoneNumber());
        }
        if (!newAccountInfo.getEmail().isEmpty()) {
            userInfo.setEmail(newAccountInfo.getEmail());
        }
        accountInfoRepository.save(userInfo);
    }

    public void changeCurrentAccountPassword(Account account, String newPassword) {
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    @Transactional
    public void changeEmployeePassword(Long id, String newPassword) {
        Account employee = accountRepository.getOne(id);
        employee.setPassword(passwordEncoder.encode(newPassword));
    }

    public Account findByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    public Account getAccountById(Long id) {
        return accountRepository.getOne(id);
    }

    public List<Account> getEmployees(Account owner) {
        List<List<Account>> listOfListsEmployees = owner.getShops().stream()
                .map(shop -> shop.getAccounts()).collect(Collectors.toCollection(ArrayList::new));
        List<Account> employees = listOfListsEmployees.stream()
                .flatMap(List::stream).filter(acc -> !acc.equals(owner))
                .collect(Collectors.toList());
        return employees;
    }

    public void validateIdAccess(Long id, Account owner) {
        if (id == null || owner == null) {
            throw new RuntimeException("Invalid user id or current account");
        }
        Account employee = accountRepository.getOne(id);
        if (id.equals(owner.getId())) {
            return;
        }
        if (owner.getAuthority() != Authority.ADMIN || !getEmployees(owner).contains(employee)) {
            throw new RuntimeException("Current account doesn't have access to this user");
        }
    }

}
