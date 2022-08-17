package com.app.bank_acquiring.fluentlenium;

import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.support.FindBy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class NavbarTest extends BaseTest {

    @FindBy(css = "nav")
    private FluentWebElement navbar;

    @Test
    public void whenClickShops_thenRedirectToShopsPage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(1)).hasTextContaining("Магазины");
        navbar.find("a").index(1).click();
        isAtShopsPage();
    }

    @Test
    public void whenClickProducts_thenRedirectToProductsPage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(2)).hasTextContaining("Товары");
        navbar.find("a").index(2).click();
        isAtProductsPage();
    }

    @Test
    public void whenClickAccounts_thenRedirectToAccountsPage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(3)).hasTextContaining("Сотрудники");
        navbar.find("a").index(3).click();
        isAtAccountsPage();
    }

    @Test
    public void whenClickTerminals_thenRedirectToTerminalsPage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(4)).hasTextContaining("Терминалы");
        navbar.find("a").index(4).click();
        isAtTerminalsPage();
    }

    @Test
    public void whenClickProfile_thenRedirectToAccountsProfilePage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(5)).hasTextContaining("useruser");
        navbar.find("a").index(5).click();
        isAtAccountProfilePage();
    }

    @Test
    public void whenClickLogout_thenRedirectToLoginPage() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        assertThat(navbar.find("a").index(6)).hasTextContaining("Выйти");
        navbar.find("a").index(6).click();
        isAtLoginPage();
    }

    @Test
    public void whenLoginHeadCashier_thenNavbarHasOnlyMain_Shop_Profile_LogoutReferences() {
        //login via head_cashier acc
        loginUser("headcashier", "12345678");
        goTo(currentProfileUrl);
        assertThat(navbar.find("a").size()).isEqualTo(4);
    }

    @Test
    public void whenResizeWindow_thenNavbarAdapt() {
        //register and login
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        goTo(mainUrl);
        Dimension standard = navbar.getElement().getSize();
        //in smaller window navbar should hide nav elements
        window().setSize(new Dimension(300, 600));
        assertThat(navbar.getElement().getSize().getHeight()).isEqualTo(standard.getHeight());
        navbar.find("button").click();
        assertThat(navbar.getElement().getSize().getHeight()).isGreaterThan(standard.getHeight());
    }
}
