package com.app.bank_acquiring.ui;


import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingTextContent;
import static org.fluentlenium.core.filter.FilterConstructor.withName;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class ProductsTest extends BaseTest {

    @FindBy(css = "table")
    private FluentWebElement prodTable;

    @Test
    public void whenSubmitForm_thenNewProductAddedInTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //opening products page
        goTo(productsUrl);
        isAtProductsPage();
        //filling form with new product
        selectProdTable(1);//select 1-st existing shop (0 is empty select)
        verifyProdTableIsEmpty();
        find("#type").fillSelect().withText("Товар");//product type (item or service), mandatory
        find("#name").fill().with("MyProduct");//mandatory input
        find("#purchasePrice").fill().with("100.10");
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(0);//mandatory select
        find("#vendorCode").fill().with("123456ABCD");
        find("#barCode").fill().with("123456789");
        find("#balance").fill().with("150");//mandatory input
        el("form").el("button", containingTextContent("Сохранить")).click();
        isAtProductsPage();
        //after reload page should contain new product in table
        assertThat(prodTable.textContent()).contains(
                "MyProduct",
                "Товар",
                "100.10",
                "123.45",
                "123456ABCD"
        );
        assertThat(prodTable.el("#balances").attribute("placeholder")).contains("150");
    }

    @Test
    public void whenClick_DeleteProduct_Button_thenProductRemovedFromTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //opening products page
        goTo(productsUrl);
        isAtProductsPage();
        //filling form with new product
        selectProdTable(1);//select 1-st existing shop (0 is empty select)
        find("#type").fillSelect().withText("Товар");//product type (item or service), mandatory
        find("#name").fill().with("MyProduct");//mandatory input
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(0);//mandatory select
        find("#balance").fill().with("150");//mandatory input
        find("form").first().submit();
        isAtProductsPage();
        //clicking checkbox of product to delete
        selectProdTable(1);
        prodTable.el("input", withName("prods")).click();
        find("form").last().el("button", containingTextContent("Удалить")).click();
        isAtProductsPage();
        //product should be vanished from table
        selectProdTable(1);
        verifyProdTableIsEmpty();
    }

    @Test
    public void whenClick_UpdateBalance_Button_thenProductBalanceUpdatedInTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //opening products page
        goTo(productsUrl);
        isAtProductsPage();
        //filling form with new product
        selectProdTable(1);//select 1-st existing shop (0 is empty select)
        find("#type").fillSelect().withText("Товар");//product type (item or service), mandatory
        find("#name").fill().with("MyProduct");//mandatory input
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(0);//mandatory select
        find("#balance").fill().with("150");//mandatory input
        find("form").first().submit();
        isAtProductsPage();
        //updating prod balance(checkbox+input new value+button)
        selectProdTable(1);
        prodTable.el("input", withName("prods")).click();//clicking checkbox
        prodTable.el("input", withName("balances")).fill().with("253");//filling balance input with new value
        find("form").last().el("button", containingTextContent("Обновить остаток")).click();
        isAtProductsPage();
        //input balance placeholder should contain new value after reload
        selectProdTable(1);
        assertThat(prodTable.el("input", withName("balances")).attribute("placeholder")).isEqualTo("253.0");
    }

    @Test
    public void whenClick_CopyTo_Button_thenProductBalanceUpdatedInTable() {
        //register->login->add two shops
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop1");
        createShop("MyShop2");
        goTo(shopsUrl);
        assertThat(pageSource()).contains("MyShop1", "MyShop2");
        //opening products page
        goTo(productsUrl);
        isAtProductsPage();
        //filling form with new product
        selectProdTable(1);//select 1-st existing shop (0 is empty select)
        find("#type").fillSelect().withText("Товар");//product type (item or service), mandatory
        find("#name").fill().with("MyProduct");//mandatory input
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(0);//mandatory select
        find("#balance").fill().with("150");//mandatory input
        find("form").first().submit();
        isAtProductsPage();
        //copying product from 1-st shop to 2-nd (checkbox+select option with other shop+button)
        selectProdTable(1);//selecting 1-st shop
        prodTable.el("input", withName("prods")).click();//clicking checkbox
        el("select", withName("targetShopId")).click();//clicking select
        keyboard().sendKeys(Keys.ARROW_DOWN).sendKeys(Keys.ENTER);//selecting 2-nd shop as target for copying product(0 index - empty option)
        find("button", containingTextContent("Копировать в")).click();
        isAtProductsPage();
        //verifying that 2-nd shop prod's table(which at the moment is last table in dom) contains copied product after reload page
        selectProdTable(2);//selecting 2-nd shop
        assertThat(find("table").last().textContent()).contains(
                "MyProduct",
                "Товар",
                "123.45"
        );
        assertThat(find("table").last().el("#balances").attribute("placeholder")).isEqualTo("0.0");
    }

    private void verifyProdTableIsEmpty() {
        if (prodTable.present()) assertThat(prodTable.textContent()).contains("Нет добавленных товаров");
    }

    private void selectProdTable(int index) {
        find("#shop").fillSelect().withIndex(index);
    }

}
