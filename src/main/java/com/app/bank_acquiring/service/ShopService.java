package com.app.bank_acquiring.service;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ShopService {
    private final Logger logger = LoggerFactory.getLogger(ShopService.class);
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
    public void bundleShopWithAccount(@NonNull Shop shop, @NonNull String accName) {
        Account account = accountRepository.findByUsername(accName);
        if (shop.getAccounts() == null) {
            List<Account> accounts = new ArrayList<>();
            accounts.add(account);
            shop.setAccounts(accounts);
        } else {
            shop.getAccounts().add(account);
        }
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
        Shop shop = shopRepository.getById(shopId);
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
        try {
                if (owner.getShops() == null || !owner.getShops().contains(shop)) {
                    logger.error("ID validation error: given account(id " + owner.getId()
                            + ") doesn't have permission to shop(id " + shop.getId() + ")");
                    throw new IdValidationException("Current account doesn't have access to this shop");
                }
        } catch (EntityNotFoundException e){
            logger.error("ID validation error: entity not exist");
            throw new IdValidationException("Current account doesn't have access to this shop");
        }

    }

    private void validateEmployeeIdAccess(Shop shop, Account employee) {
        try {
                if (shop.getAccounts() == null || !shop.getAccounts().contains(employee)) {
                    logger.error("ID validation error: given account(id "
                            + employee.getId() + ") doesn't belong to shop(id " + shop.getId() + ")");
                    throw new IdValidationException("Current shop doesn't have access to this employee");
                }
        } catch (EntityNotFoundException e){
            logger.error("ID validation error: entity not exist");
            throw new IdValidationException("Current shop doesn't have access to this employee");
        }
    }

}
