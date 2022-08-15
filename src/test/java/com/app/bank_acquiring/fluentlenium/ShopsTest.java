package com.app.bank_acquiring.fluentlenium;


import com.app.bank_acquiring.repository.AccountInfoRepository;
import com.app.bank_acquiring.repository.AccountRepository;
import com.app.bank_acquiring.repository.ShopRepository;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import javax.persistence.EntityManager;
import java.util.concurrent.TimeUnit;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class ShopsTest extends BaseTest {

    @FindBy(css = "table")
    private FluentWebElement shopsTable;

    @Test
    public void whenSubmit_thenNewShopAddedIntoTable() {
        //register and login
        registerUser("useruser", "password");
        loginUser("useruser", "password");
        //going to shops url
        goTo(shopsUrl);
        isAtShopsPage();
        //shops table should be empty at the beginning
        verifyShopsTableIsEmpty();
        //filling form with new shop and submit
        find("#name").fill().with("MyNewShop");
        find("#city").fill().with("MyCity");
        find("#address").fill().with("MyAddress");
        find("button").click();
        //should reload page and display new shop in the table
        await().atMost(1, TimeUnit.SECONDS);
        isAtShopsPage();
        assertThat(shopsTable.textContent()).contains("MyNewShop");
    }

    @Test
    public void givenBlankShopName_whenSubmit_thenReloadPageWithError() {
        //register and login
        registerUser("useruser", "password");
        loginUser("useruser", "password");
        //going to shops url
        goTo(shopsUrl);
        isAtShopsPage();
        //shops table should be empty at the beginning
        verifyShopsTableIsEmpty();
        //filling form with invalid data and submit
        find("#name").fill().with(" ");
        find("#city").fill().with("MyCity");
        find("#address").fill().with("MyAddress");
        find("button").click();
        //should reload page and display validation errors
        await().atMost(1, TimeUnit.SECONDS);
        isAtShopsPage();
        verifyShopsTableIsEmpty();
        assertThat(pageSource()).contains("Название магазина не может состоять из пробелов");
    }

    @Test
    public void givenInvalidCityAddress_whenSubmit_thenTrimInputValues() {
        //register and login
        registerUser("useruser", "password");
        loginUser("useruser", "password");
        //going to shops url
        goTo(shopsUrl);
        isAtShopsPage();
        //shops table should be empty at the beginning
        verifyShopsTableIsEmpty();
        //filling form with invalid city, address and submit
        find("#name").fill().with("MyShop");
        find("#city").fill().with("MyTooooooooooooooLoooooooooongCityIsMoreThanFortyLetters");
        find("#address").fill().with("MyToooooooooooooooLooooooooooooooooooooooooooongAddressIsMoreThanSixtyLetters");
        //should trim excessive string due to html input element constraints
        assertThat(el("#city").value().length()).isEqualTo(40);
        assertThat(el("#address").value().length()).isEqualTo(60);
    }

    @Test
    public void whenDelete_thenShopNotPresentAfterReload() {
        //register and login
        registerUser("useruser", "password");
        loginUser("useruser", "password");
        //going to shops url
        goTo(shopsUrl);
        isAtShopsPage();
        //shops table should be empty at the beginning
        verifyShopsTableIsEmpty();
        //filling form with new shop and submit
        find("#name").fill().with("MyNewShop");
        find("#city").fill().with("MyCity");
        find("#address").fill().with("MyAddress");
        find("button").click();
        //should reload page and display new shop in the table
        await().atMost(1, TimeUnit.SECONDS);
        isAtShopsPage();
        assertThat(shopsTable.textContent()).contains("MyNewShop");
        //deleting shop by clicking button inside table row
        find("input").last().submit();
        //shops table should be empty after reload
        await().atMost(1, TimeUnit.SECONDS);
        isAtShopsPage();
        verifyShopsTableIsEmpty();
    }

    private void verifyShopsTableIsEmpty() {
        assertThat(shopsTable.textContent()).contains("Нет добавленных магазинов");
    }


}
