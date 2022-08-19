package com.app.bank_acquiring.ui;


import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private FluentWebElement prodTables;

    @Test
    public void whenSubmitForm_thenNewProductAddedInTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //opening products page
        goTo(productsUrl);
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
        //find("form").first().submit();
        el("form").el("button",containingTextContent("Сохранить")).click();
        //after reload page should contain new product in table
        assertThat(prodTables.textContent()).contains(
                "MyProduct",
                "Товар",
                "100.10",
                "123.45",
                "123456ABCD"
        );
        assertThat(prodTables.el("#balances").attribute("placeholder")).contains("150");
    }

    @Test
    public void whenClickDeleteProdButton_thenProductRemovedFromTable(){
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //opening products page
        goTo(productsUrl);
        //filling form with new product
        selectProdTable(1);//select 1-st existing shop (0 is empty select)
        find("#type").fillSelect().withText("Товар");//product type (item or service), mandatory
        find("#name").fill().with("MyProduct");//mandatory input
        find("#sellingPrice").fill().with("123.45");//mandatory input
        find("#measurementUnit").fillSelect().withIndex(0);//mandatory select
        find("#balance").fill().with("150");//mandatory input
        find("form").first().submit();
        //clicking checkbox of product to delete
        //window().setPosition()
        selectProdTable(1);
        prodTables.el("input",withName("prods")).click();
        find("form").last().el("button",containingTextContent("Удалить")).click();
        //product should be vanished from table
        selectProdTable(1);
        verifyProdTableIsEmpty();
    }

    private void verifyProdTableIsEmpty() {
        if (prodTables.present()) assertThat(prodTables.textContent()).contains("Нет добавленных товаров");
    }

    private void selectProdTable(int index){
        find("#shop").fillSelect().withIndex(index);
    }

}
