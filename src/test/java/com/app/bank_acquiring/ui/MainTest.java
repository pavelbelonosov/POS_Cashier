package com.app.bank_acquiring.ui;

import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.support.FindBy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class MainTest extends BaseTest {

    @FindBy(css = "#jumbotronChequeArea")
    private FluentWebElement chequeArea;

    @FindBy(css = "#dataTable")
    private FluentWebElement prodTable;

    @Test
    public void givenNoWorkingTerminal_whenOpenMainPage_thenPageIndicatesAboutIt() {
        //register->login->add shop->add terminal
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        goTo(mainUrl);
        //main page should contain info for user to choose and set any terminal as working
        assertThat(pageSource()).contains("Добавьте и выберите рабочий терминал");
    }

    @Test
    public void whenClick_AddInCart_Button_thenProductIsAddedInCart() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable, prodCart,chequeArea
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("1");//one product to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //cart should contain product
        assertThat(el("a", withName("productInCart")).textContent()).contains("MyProduct");
    }

    @Test
    public void whenClickClearCheque_thenCartIsCleared() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("1");//one product to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //cart should contain product
        FluentWebElement productInCart = el("a", withName("productInCart"));
        assertThat(productInCart.textContent()).contains("MyProduct");
        //after reload, product should be vanished
        el("a", containingTextContent("Очистить чек")).click();
        isAtMainPage();
        assertThat(productInCart).isNotPresent();
    }

    @Test
    public void whenClickOnProductInCart_thenProductAmountInCartIsDecreased() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("2");//two products to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //cart should contain product with amount=2
        FluentWebElement productInCart = el("a", withName("productInCart"));
        assertThat(productInCart.textContent()).contains("MyProduct | 2");
        //product amount should be decreased by one
        productInCart.click();
        isAtMainPage();
        assertThat(productInCart.textContent()).contains("MyProduct | 1");
    }

    @Test
    public void whenClickPaymentButton_thenChequeAreaContainsCheque() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("2");//two products to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //after payment, cheque should be formed
        el("#paymentBtn").click();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        assertThat(chequeArea.textContent()).contains("MyProduct");
    }

    @Test
    public void whenClickRefundButton_thenChequeAreaContainsCheque() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("1");//one product to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //after refund, cheque should be formed
        el("#refundBtn").click();
        alert().accept();//when refund alert pops up
        await().explicitlyFor(1, TimeUnit.SECONDS);
        assertThat(chequeArea.textContent()).contains("MyProduct");
    }

    @Test
    public void whenClickReportsButton_thenModalWithSalesInfoIsOpened() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //selecting product to sell, fill amount input and click button
        prodTable.el("input", withName("prods")).click();//checkbox
        prodTable.el("input", withName("quantity")).fill().with("2");//two products to sell
        find("button", containingText("Добавить в чек")).click();
        isAtMainPage();
        //after payment, cheque should be formed
        el("#paymentBtn").click();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        assertThat(chequeArea.textContent()).contains("MyProduct");
        //making reports query->modal should contain sales info
        executeScript("getTransactionsStat();");//workaround, because button "Reports" is not reachable
        await().explicitlyFor(1, TimeUnit.SECONDS);
        assertThat(el("#currentBalance").textContent()).isNotEmpty();
        assertThat(el("#salesStat").textContent()).isNotEmpty();
        assertThat(el("#refundsStat").textContent()).isNotEmpty();
    }

    @Test
    public void whenClickXreportButton_thenChequeAreaContainReportCheque() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //clicking reports button ->modal pops up->clicking xreport button
        el("button", containingText("Отчеты")).click();
        await().explicitlyFor(3, TimeUnit.SECONDS);
        el("#xReportBtn").click();
        await().explicitlyFor(3, TimeUnit.SECONDS);
        //cheque should contain info
        assertThat(chequeArea.textContent()).contains("ПРОМЕЖУТОЧНЫЙ ОТЧЕТ");
    }

    @Test
    public void whenClickZreportButton_thenChequeAreaContainReportCheque() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //clicking reports button ->modal pops up->clicking zreport button
        el("button", containingText("Отчеты")).click();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        el("#zReportBtn").click();
        alert().accept();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        //cheque should contain info
        assertThat(chequeArea.textContent()).contains("ОТЧЕТ О ЗАКРЫТИИ СМЕНЫ");
    }

    @Test
    public void whenSendChequeToEmail_thenAlertPopsUp() {
        //register->login->add shop->add terminal->take terminal to work
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createProduct("MyProduct", 0);
        createTerminalPos("12345678", true, 0);
        takeTerminalToWork("12345678");
        //when there is working terminal -> main page should contain prodTable
        assertThat(prodTable).isPresent();
        //clicking reports button ->modal pops up->clicking xreport button
        el("button", containingText("Отчеты")).click();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        el("#xReportBtn").click();
        await().explicitlyFor(1, TimeUnit.SECONDS);
        el(".close").click();//close modal
        //cheque should contain info
        assertThat(chequeArea.textContent()).contains("ПРОМЕЖУТОЧНЫЙ ОТЧЕТ");
        //filling email input+imitating button send cheque by js->alert should pop up
        find("#emailFormInputGroup").fill().withText("user@user.com");
        executeScript("sendChequeToClient();");
        await().explicitlyFor(3, TimeUnit.SECONDS);
        assertThat(alert().present()).isTrue();
    }

}
