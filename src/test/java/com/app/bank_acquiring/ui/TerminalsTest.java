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
public class TerminalsTest extends BaseTest {

    @FindBy(css = "table")
    private FluentWebElement terminalsTable;

    @Test
    public void givenIntegratedPos_whenSubmitForm_thenNewTerminalIsAddedInTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to terminals url
        goTo(terminalsUrl);
        isAtTerminalsPage();
        verifyTableIsEmpty();
        //filling form for IKR POS and submit
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#ip").fill().with("1.2.3.4");//mandatory input for ikr
        el("#chequeHeader").fill().with("Cheque | Header"); //input for ikr only
        el("#shop").fillSelect().withIndex(0);//mandatory select(selecting only one existing shop)
        el("button", containingTextContent("Сохранить")).click();
        isAtTerminalsPage();
        //table should contain new terminal after reloading page
        assertThat(terminalsTable.textContent()).contains(
                "12345678",
                "123456789012",
                "1.2.3.4",
                "Cheque | Header"
        );
    }

    @Test
    public void givenStandalonePos_whenSubmitForm_thenNewTerminalIsAddedInTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to terminals url
        goTo(terminalsUrl);
        isAtTerminalsPage();
        verifyTableIsEmpty();
        //filling form for standalone POS and submit
        el("#standalone").click();//checkbox with standalone/ikr
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#shop").fillSelect().withIndex(0);//mandatory select(selecting only one existing shop)
        el("button", containingTextContent("Сохранить")).click();
        isAtTerminalsPage();
        //table should contain new terminal after reloading page
        assertThat(terminalsTable.textContent()).contains(
                "12345678",
                "123456789012"
        );
        //cell in table for standalone setting should contain "yes"
        assertThat(find("tr").get(1).find("td").get(2).textContent()).isEqualTo("Да");
    }

    @Test
    public void whenClick_TakeTerminalToWor_Button_thenPageRedirectsToMain() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to terminals url
        goTo(terminalsUrl);
        isAtTerminalsPage();
        verifyTableIsEmpty();
        //filling form and submit
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#ip").fill().with("1.2.3.4");//mandatory input for ikr
        el("#chequeHeader").fill().with("Cheque | Header"); //input for ikr only
        el("#shop").fillSelect().withIndex(0);//mandatory select(selecting only one existing shop)
        el("button", containingTextContent("Сохранить")).click();
        isAtTerminalsPage();
        //selecting terminal for work and click button
        el("select", withName("terminalId")).fillSelect().withIndex(0);
        el("button", containingTextContent("Встать за кассу")).click();
        //should redirect to main page
        isAtMainPage();
        //cell in table with "Terminal at Work" setting also should contain "Yes"
        goTo(terminalsUrl);
        assertThat(find("tr").get(1).find("td").get(5).textContent()).isEqualTo("Да");
    }

    @Test
    public void whenClick_Delete_Button_thenRemovesTerminalFromTable() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to terminals url
        goTo(terminalsUrl);
        isAtTerminalsPage();
        verifyTableIsEmpty();
        //filling form and submit
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#ip").fill().with("1.2.3.4");//mandatory input for ikr
        el("#chequeHeader").fill().with("Cheque | Header"); //input for ikr only
        el("#shop").fillSelect().withIndex(0);//mandatory select(selecting only one existing shop)
        el("button", containingTextContent("Сохранить")).click();
        isAtTerminalsPage();
        //submitting delete form-> terminal should be vanished from table
        terminalsTable.el("form").submit();
        isAtTerminalsPage();
        verifyTableIsEmpty();
    }

    @Test
    public void whenClickTerminalTidInTable_thenRdirectToTerminalSettingPage() {
        //register->login->add shop
        registerAdminUser("useruser", "password");
        loginUser("useruser", "password");
        createShop("MyShop");
        //going to terminals url
        goTo(terminalsUrl);
        isAtTerminalsPage();
        verifyTableIsEmpty();
        //filling form and submit
        el("#tid").fill().with("12345678");//mandatory input
        el("#mid").fill().with("123456789012");//mandatory input
        el("#ip").fill().with("1.2.3.4");//mandatory input for ikr
        el("#chequeHeader").fill().with("Cheque | Header"); //input for ikr only
        el("#shop").fillSelect().withIndex(0);//mandatory select(selecting only one existing shop)
        el("button", containingTextContent("Сохранить")).click();
        isAtTerminalsPage();
        //clicking terminal tid href-> should redirect to Terminal Settings page
        terminalsTable.el("td", containingTextContent("12345678")).click();
        isAtTerminalPage();
    }

    private void verifyTableIsEmpty() {
        assertThat(terminalsTable.textContent()).contains("Нет добавленных терминалов");
    }
}
