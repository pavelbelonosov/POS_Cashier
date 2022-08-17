package com.app.bank_acquiring.integration;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.Terminal;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.domain.account.AccountInfo;
import com.app.bank_acquiring.domain.account.Authority;
import com.app.bank_acquiring.domain.product.MeasurementUnit;
import com.app.bank_acquiring.domain.product.Product;
import com.app.bank_acquiring.domain.product.Type;
import com.app.bank_acquiring.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ActiveProfiles("test")
@Service
@AllArgsConstructor
public class UtilPopulate {

    private AccountRepository accountRepository;
    private AccountInfoRepository accountInfoRepository;
    private ShopRepository shopRepository;
    private ProductRepository productRepository;
    private TerminalRepository terminalRepository;
    private SalesCounterRepository salesCounterRepository;
    private TransactionRepository transactionRepository;
    private PasswordEncoder passwordEncoder;

    public void clearTables() {
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
        productRepository.deleteAll();
        transactionRepository.deleteAll();
        terminalRepository.deleteAll();
        salesCounterRepository.deleteAll();
    }

    public Account createUser(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(passwordEncoder.encode("password"));
        user.setAuthority(authority);
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccount(user);
        accountInfoRepository.save(accountInfo);
        return accountRepository.save(user);
    }

    public Shop createShopForAdmin(Account admin) {
        Shop shop = new Shop();
        shop.setName("shop");
        List<Account> accountList = new ArrayList<>();
        accountList.add(admin);
        shop.setAccounts(accountList);
        return shopRepository.save(shop);
    }

    public Product createProductForShop(Shop shop) {
        Product product = new Product();
        product.setShop(shop);
        product.setName("product");
        product.setType(Type.ITEM);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        product.setPurchasePrice(new BigDecimal("123.45"));
        product.setSellingPrice(new BigDecimal("123.45"));
        return productRepository.save(product);
    }

    public Terminal createTerminalForShop(Shop shop, Account account) {
        Terminal terminal = new Terminal();
        terminal.setTid((new Random().nextInt(1000) + 10000000) + "");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        terminal.setStandalone(false);//integrated pos
        terminal.setShop(shop);
        terminal.setAccount(account);
        return terminalRepository.save(terminal);
    }

    /**
     * The method is used to create Account object, which is not persisted in repository yet.
     * Mainly used as thymeleaf object, when testing post requests from client to create new Account.
     *
     * @param authority Account AUTHORITY
     * @return not persisted Account
     */
    public Account createDetachedUser(Authority authority) {
        Account user = new Account();
        user.setUsername("username" + new Random().nextInt(Integer.MAX_VALUE));
        user.setPassword(passwordEncoder.encode("password"));
        user.setAuthority(authority);
        return user;
    }

    /**
     * The method is used to create Shop object, which is not persisted in repository yet.
     * Mainly used as thymeleaf object, when testing post requests from client to create new Shop.
     *
     * @return not persisted Shop
     */
    public Shop createDetachedShop() {
        Shop shop = new Shop();
        shop.setName("shop");
        return shop;
    }

    /**
     * The method is used to create Product object, which is not persisted in repository yet.
     * Mainly used as thymeleaf object, when testing post requests from client to create new Product.
     *
     * @param type Product TYPE
     * @return not persisted Product
     */
    public Product createDetachedProduct(Type type) {
        Product product = new Product();
        product.setName("product");
        product.setType(type);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        return product;
    }

    /**
     * The method is used to create Terminal object, which is not persisted in repository yet.
     * Mainly used as thymeleaf object, when testing post requests from client to create new Terminal.
     *
     * @return not persisted Terminal
     */
    public Terminal createDetachedTerminal() {
        Terminal terminal = new Terminal();
        terminal.setStandalone(false);
        terminal.setTid((new Random().nextInt(1000) + 10000000) + "");
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        return terminal;
    }

    /**
     * The method is used to create Terminal object, which belongs to given shop and is not persisted in repository yet.
     * Mainly used as thymeleaf object, when testing post requests from client to create new Terminal.
     * @param shop Shop to which Terminal is bundled
     * @return not persisted Terminal
     */
    public Terminal createDetachedTerminalForShop(Shop shop) {
        Terminal terminal = new Terminal();
        terminal.setTid("00000000");
        terminal.setStandalone(true);
        terminal.setIp("1.1.1.1");
        terminal.setMid("123456789000");
        terminal.setChequeHeader("header");
        terminal.setShop(shop);
        return terminal;
    }

    public List<SimpleGrantedAuthority> getAuthorities(Account account) {
        return List.of(new SimpleGrantedAuthority(account.getAuthority().toString()));
    }

}
