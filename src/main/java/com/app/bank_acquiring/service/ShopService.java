package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ShopService {

    private AccountRepository accountRepository;
    private AccountInfoRepository accountInfoRepository;
    private ShopRepository shopRepository;
    private UposService uposService;

    @Transactional
    public Shop getShop(@NonNull Long id, @NonNull String currentUser) {
        Account current = accountRepository.findByUsername(currentUser);
        Shop shop = shopRepository.getOne(id);
        validateShopIdAccess(shop, current);
        return shop;
    }

    @Transactional
    public void createShop(@NonNull Shop shop, @NonNull String currentUser) {
        Account owner = accountRepository.findByUsername(currentUser);
        List<Account> accounts = new ArrayList<>();
        accounts.add(owner);
        shop.setAccounts(accounts);
        shopRepository.save(shop);
    }

    @Transactional
    public void deleteAccountFromShop(@NonNull Long shopId, @NonNull Long accountId, @NonNull String currentUser) {
        Account owner = accountRepository.findByUsername(currentUser);
        Shop shop = shopRepository.getOne(shopId);
        validateShopIdAccess(shop, owner);
        Account employee = accountRepository.getOne(accountId);
        validateEmployeeIdAccess(shop, employee);
        AccountInfo accountInfo = employee.getAccountInfo();
        shop.getAccounts().remove(employee);
        accountInfoRepository.delete(accountInfo);
        accountRepository.delete(employee);
    }

    @Transactional
    public void deleteShop(@NonNull Long shopId, @NonNull String currentUser) {
        Account owner = accountRepository.findByUsername(currentUser);
        Shop shop = shopRepository.getOne(shopId);
        validateShopIdAccess(shop, owner);
        shop.getAccounts().removeIf(account -> {
            if (!account.getId().equals(owner.getId())) {
                accountInfoRepository.delete(account.getAccountInfo());
                accountRepository.delete(account);
            }
            return false;
        });
        shop.getTerminals().forEach(terminal -> uposService.deleteUserUpos(owner.getId(), shopId, terminal.getTid()));
        shopRepository.delete(shop);
    }

    private void validateShopIdAccess(Shop shop, Account owner) {
        if (shop != null) {
            if (owner == null || owner.getShops() == null || !owner.getShops().contains(shop)) {
                throw new RuntimeException("Current account doesn't have access to this shop");
            }
        }
    }

    private void validateEmployeeIdAccess(Shop shop, Account employee) {
        if (shop != null && employee != null) {
            if (shop.getAccounts() == null || !shop.getAccounts().contains(employee)) {
                throw new RuntimeException("Current shop doesn't have access to this employee");
            }
        }
    }


}
