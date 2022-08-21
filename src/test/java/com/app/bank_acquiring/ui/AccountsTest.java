package com.app.bank_acquiring.ui;

import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.support.FindBy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;
import static org.fluentlenium.core.filter.FilterConstructor.containingTextContent;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class AccountsTest extends BaseTest {

    @FindBy(css = "table")
    private FluentWebElement employeesTable;

    @Test
    public void whenSubmitEmployee_thenNewEmployeeIsAddedIntoTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to accounts url
        goTo(accountsUrl);
        isAtAccountsPage();
        verifyAccountTableIsEmpty();
        //filling form to create new employee
        find("#username").fill().with("MyEmployee");//mandatory input
        find("#password").fill().with("password");//mandatory input
        find("#firstName").fill().with("John");
        find("#lastName").fill().with("Doe");
        find("#telephoneNumber").fill().with("9991113344");//mandatory input
        find("#shop").click().fillSelect().withIndex(0);//mandatory select
        find("#authority").click().fillSelect().withIndex(0);//mandatory select
        find("form").submit();
        //page should be reloaded with shop info
        isAtAccountsPage();
        assertThat(employeesTable.textContent()).contains(
                "MyEmployee",
                "John Doe",
                "MyShop",
                "9991113344",
                "Старший кассир"
        );
    }

    @Test
    public void givenNoAvailableShop_whenSubmitEmployee_thenFormButtonIsNotActive() {
        //register->login->no shop added
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        //going to accounts url
        goTo(accountsUrl);
        isAtAccountsPage();
        //submit button should not be interactive
        assertThat(el("button", containingTextContent("Сохранить"))).isNotClickable();

    }

    @Test
    public void givenNotValidInputs_whenSubmitEmployee_thenReloadsWithErrors() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to accounts url
        goTo(accountsUrl);
        isAtAccountsPage();
        verifyAccountTableIsEmpty();
        //filling form to create new employee
        find("#username").fill().with("useruser");//username should be free
        find("#password").fill().with("           ");//blank not allowed
        find("#telephoneNumber").fill().with("9991113344");//mandatory input
        find("#shop").click().fillSelect().withIndex(0);//mandatory select
        find("#authority").click().fillSelect().withIndex(0);//mandatory select
        find("form").submit();
        //page should be reloaded with shop info
        isAtAccountsPage();
        assertThat(pageSource()).contains(
                "Логин занят",
                "Пароль не может быть пустым"
        );
    }

    @Test
    public void whenDeleteEmployee_thenEmployeeIsDeletedFromTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to accounts url
        goTo(accountsUrl);
        isAtAccountsPage();
        verifyAccountTableIsEmpty();
        //filling form to create new employee
        find("#username").fill().with("MyEmployee");//mandatory input
        find("#password").fill().with("password");//mandatory input
        find("#telephoneNumber").fill().with("9991113344");//mandatory input
        find("#shop").click().fillSelect().withIndex(0);//mandatory select
        find("#authority").click().fillSelect().withIndex(0);//mandatory select
        find("form").submit();
        //page should be reloaded with shop info
        isAtAccountsPage();
        assertThat(employeesTable.textContent()).contains("MyEmployee");
        //submit delete form in table
        employeesTable.find("form").submit();
        //employee should be vanished
        verifyAccountTableIsEmpty();
    }

    @Test
    public void whenClickOnEmployeeUsername_thenGoToEmployeeProfilePage(){
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to accounts url
        goTo(accountsUrl);
        isAtAccountsPage();
        verifyAccountTableIsEmpty();
        //filling form to create new employee
        find("#username").fill().with("MyEmployee");//mandatory input
        find("#password").fill().with("password");//mandatory input
        find("#telephoneNumber").fill().with("9991113344");//mandatory input
        find("#shop").click().fillSelect().withIndex(0);//mandatory select
        find("#authority").click().fillSelect().withIndex(0);//mandatory select
        find("form").submit();
        isAtAccountsPage();
        //clicking href with employee username
        el("td",containingTextContent("MyEmployee")).click();
        isAtAccountProfilePage();
    }

    @Test
    public void test(){
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        createEmployee("MySlave", 0);
    }

    private void verifyAccountTableIsEmpty() {
        assertThat(employeesTable.textContent()).contains("Нет добавленных сотрудников");
    }
}
