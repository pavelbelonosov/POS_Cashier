package com.app.bank_acquiring.ui;

import com.app.bank_acquiring.repository.*;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.configuration.FluentConfiguration;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingTextContent;
import static org.fluentlenium.core.filter.FilterConstructor.withName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FluentConfiguration(webDriver = "remote", remoteUrl = "http://chrome:4444", capabilities = "{\"goog:chromeOptions\": {\"args\": [" +
                "\"headless\"," +
                "\"disable-gpu\"," +
                "\"window-size=1920,1080\"]}}")//webDriver = "chrome",

public abstract class BaseTest extends FluentTest {
    /*
    static {
        WebDriverManager.chromedriver().setup(); //setup webdriver locally with bonigarcia lib
    }*/

    @LocalServerPort
    protected Integer port;
    protected String hostUrl;
    protected String registrationUrl, regPageTitle;
    protected String loginUrl, loginPageTitle;
    protected String mainUrl, mainPageTitle;
    protected String shopsUrl, shopsPageTitle;
    protected String productsUrl, productsPageTitle;
    protected String accountsUrl, accountsPageTitle;
    protected String terminalsUrl, terminalsPageTitle, terminalPageTitle;
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
        hostUrl = "http://" + System.getenv("APP_HOSTNAME") + ":" + port;
        registrationUrl = hostUrl + "/accounts/registration";
        loginUrl = hostUrl + "/login";
        mainUrl = hostUrl + "/main";
        shopsUrl = hostUrl + "/shops";
        productsUrl = hostUrl + "/products";
        accountsUrl = hostUrl + "/accounts";
        terminalsUrl = hostUrl + "/terminals";
        currentProfileUrl = hostUrl + "/accounts/current";
        logoutUrl = hostUrl + "/logout";
        regPageTitle = "POS-???????????? | ??????????????????????";
        loginPageTitle = "POS-???????????? | ????????";
        mainPageTitle = "POS-???????????? | ??????????";
        shopsPageTitle = "POS-???????????? | ????????????????";
        productsPageTitle = "POS-???????????? | ???????? ??????????????";
        accountsPageTitle = "POS-???????????? | ????????????????????";
        terminalsPageTitle = "POS-???????????? | ??????????????????";
        terminalPageTitle = "POS-???????????? | ????????????????";
        accountProfilePageTitle = "POS-???????????? | ??????????????";
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

    protected void createShop(String username) {
        goTo(shopsUrl);
        isAtShopsPage();
        find("#name").fill().with(username);
        find("#city").fill().with("MyCity");
        find("#address").fill().with("MyAddress");
        find("button").click();
        isAtShopsPage();
        assertThat(pageSource()).contains(username);
    }

    protected void createProduct(String prodName, int shopSelectIndex) {
        goTo(productsUrl);
        isAtProductsPage();
        //filling form with new product
        find("#shop").fillSelect().withIndex(shopSelectIndex + 1);//0 index is empty option
        find("#type").fillSelect().withText("??????????");//product type (item or service), mandatory
        find("#name").fill().with(prodName);//mandatory input
        find("#purchasePrice").fill().with("100.10");
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(shopSelectIndex);//mandatory select
        find("#vendorCode").fill().with("123456ABCD");
        find("#barCode").fill().with("123456789");
        find("#balance").fill().with("150");//mandatory input
        el("form").el("button", containingTextContent("??????????????????")).click();
        isAtProductsPage();
    }

    protected void createEmployee(String employeeUsername, int shopIndex) {
        goTo(accountsUrl);
        isAtAccountsPage();
        find("#username").fill().with(employeeUsername);//mandatory input
        find("#password").fill().with("password");//mandatory input
        find("#firstName").fill().with("John");
        find("#lastName").fill().with("Doe");
        find("#telephoneNumber").fill().with("9991113344");//mandatory input
        find("#shop").click().fillSelect().withIndex(shopIndex);//mandatory select
        find("#authority").click().fillSelect().withIndex(0);//mandatory select
        find("form").submit();
        isAtAccountsPage();
        assertThat(pageSource()).contains(employeeUsername);
    }

    protected void createTerminalPos(String terminalTid, boolean standalone, int shopSelectIndex) {
        goTo(terminalsUrl);
        isAtTerminalsPage();
        //filling form for IKR POS and submit
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#shop").fillSelect().withIndex(shopSelectIndex);//mandatory select(selecting only one existing shop)

        if (standalone) {
            el("#standalone").click();//checkbox with standalone/ikr
        } else {
            el("#ip").fill().with("1.2.3.4");//mandatory input for ikr
            el("#chequeHeader").fill().with("Cheque | Header"); //input for ikr only
        }

        el("button", containingTextContent("??????????????????")).click();
        isAtTerminalsPage();
        assertThat(pageSource()).contains(terminalTid);
    }

    protected void takeTerminalToWork(String terminalTid) {
        goTo(terminalsUrl);
        isAtTerminalsPage();
        el("select", withName("terminalId")).fillSelect().withText(terminalTid);
        el("button", containingTextContent("???????????? ???? ??????????")).click();
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
        assertThat(window().title()).isEqualTo(loginPageTitle);
    }

    protected void isAtRegistrationPage() {
        assertThat(window().title()).isEqualTo(regPageTitle);
    }

    protected void isAtMainPage() {
        assertThat(window().title()).isEqualTo(mainPageTitle);
    }

    protected void isAtShopsPage() {
        assertThat(window().title()).isEqualTo(shopsPageTitle);
    }

    protected void isAtProductsPage() {
        assertThat(window().title()).isEqualTo(productsPageTitle);
    }

    protected void isAtAccountsPage() {
        assertThat(window().title()).isEqualTo(accountsPageTitle);
    }

    protected void isAtTerminalsPage() {
        assertThat(window().title()).isEqualTo(terminalsPageTitle);
    }

    protected void isAtTerminalPage() {
        assertThat(window().title()).isEqualTo(terminalPageTitle);
    }

    protected void isAtAccountProfilePage() {
        assertThat(window().title()).isEqualTo(accountProfilePageTitle);
    }

}
