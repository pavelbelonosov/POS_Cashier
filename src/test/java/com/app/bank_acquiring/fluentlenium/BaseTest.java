package com.app.bank_acquiring.fluentlenium;

import com.app.bank_acquiring.domain.Shop;
import com.app.bank_acquiring.domain.account.Account;
import com.app.bank_acquiring.repository.*;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.configuration.FluentConfiguration;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import io.github.bonigarcia.wdm.WebDriverManager;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FluentConfiguration(webDriver = "chrome", capabilities = "{\"chromeOptions\": {\"args\": [\"headless\",\"disable-gpu\"]}}")
public abstract class BaseTest extends FluentTest {

    static {
        WebDriverManager.chromedriver().setup();
    }

    @LocalServerPort
    protected Integer port;
    protected String host;
    protected String registrationUrl, regPageTitle;
    protected String loginUrl, loginPageTitle;
    protected String mainUrl, mainPageTitle;
    protected String shopsUrl, shopsPageTitle;
    protected String productsUrl, productsPageTitle;
    protected String accountsUrl, accountsPageTitle;
    protected String terminalsUrl, terminalsPageTitle;
    protected String currentProfileUrl, accountProfilePageTitle;
    protected String logoutUrl;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountInfoRepository accountInfoRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TerminalRepository terminalRepository;
    @Autowired
    private SalesCounterRepository salesCounterRepository;


    @Before
    public void setUp() {
        clearTables();
        host = "http://localhost:" + port;
        registrationUrl = host + "/accounts/registration";
        loginUrl = host + "/login";
        mainUrl = host + "/main";
        shopsUrl = host + "/shops";
        productsUrl = host + "/products";
        accountsUrl = host + "/accounts";
        terminalsUrl = host + "/terminals";
        currentProfileUrl = host + "/accounts/current";
        logoutUrl = host + "/logout";
        regPageTitle = "POS-кассир | Регистрация";
        loginPageTitle = "POS-кассир | Вход";
        mainPageTitle = "POS-кассир | Касса";
        shopsPageTitle = "POS-кассир | Магазины";
        productsPageTitle = "POS-кассир | База товаров";
        accountsPageTitle = "POS-кассир | Сотрудники";
        terminalsPageTitle = "POS-кассир | Терминалы";
        accountProfilePageTitle = "POS-кассир | Аккаунт";
    }

    @After
    public void tearDown() {
        clearTables();
    }

    protected void registerAdminUser(String username, String pwd) {
        goTo(registrationUrl);
        isAtRegistrationPage();
        find("#username").fill().with(username);
        find("#password").fill().with(pwd);
        find("#repeatPWD").fill().with(pwd);
        find("#email").fill().with(username + "@user.ru");
        find("button").click();
        isAtLoginPage();
    }

    protected void loginUser(String username, String pass) {
        goTo(loginUrl);
        find("#username").fill().with(username);
        find("#password").fill().with(pass);
        find("button").click();
        isAtMainPage();
    }

    protected void clearTables() {
        shopRepository.deleteAll();
        accountRepository.deleteAll();
        accountInfoRepository.deleteAll();
        productRepository.deleteAll();
        transactionRepository.deleteAll();
        terminalRepository.deleteAll();
        salesCounterRepository.deleteAll();
    }

    protected Account getAcc(String user){
       // System.out.println(accountRepository.findAll().size());
        return accountRepository.findByUsername(user);
    }

    protected Shop createShopWithEmployees(Account account){

        return shopRepository.findById(4L).orElse(null);
    }


    protected void isAtLoginPage() {
        assertThat(window().title()).contains(loginPageTitle);
    }

    protected void isAtRegistrationPage() {
        assertThat(window().title()).contains(regPageTitle);
    }

    protected void isAtMainPage() {
        assertThat(window().title()).contains(mainPageTitle);
    }

    protected void isAtShopsPage() {
        assertThat(window().title()).contains(shopsPageTitle);
    }

    protected void isAtProductsPage() {
        assertThat(window().title()).isEqualTo(productsPageTitle);
    }

    protected void isAtAccountsPage() {
        assertThat(window().title()).isEqualTo(accountsPageTitle);
    }

    protected void isAtTerminalsPage(){
        assertThat(window().title()).isEqualTo(terminalsPageTitle);
    }

    protected void isAtAccountProfilePage(){
        assertThat(window().title()).isEqualTo(accountProfilePageTitle);
    }

}
