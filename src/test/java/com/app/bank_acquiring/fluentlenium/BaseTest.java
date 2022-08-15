package com.app.bank_acquiring.fluentlenium;

import com.app.bank_acquiring.repository.*;
import org.fluentlenium.adapter.junit.FluentTest;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;


import javax.persistence.EntityManager;

import java.util.Arrays;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseTest extends FluentTest {

    @LocalServerPort
    protected Integer port;
    protected String host;
    protected String registrationUrl, regPageTitle;
    protected String loginUrl, loginPageTitle;
    protected String mainUrl, mainPageTitle;
    protected String shopsUrl, shopsPageTitle;

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
        regPageTitle = "POS-кассир | Регистрация";
        loginPageTitle = "POS-кассир | Вход";
        mainPageTitle = "POS-кассир | Касса";
        shopsPageTitle = "POS-кассир | Магазины";
    }

    @After
    public void tearDown() {
        clearTables();
    }

    protected void registerUser(String username, String pwd) {
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

}
