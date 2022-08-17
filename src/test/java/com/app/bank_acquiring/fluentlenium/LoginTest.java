package com.app.bank_acquiring.fluentlenium;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;

import java.util.concurrent.TimeUnit;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class LoginTest extends BaseTest {

    @Test
    public void whenSubmit_thenRedirectToMainPage() {
        registerAdminUser("useruser","password");
        goTo(loginUrl);
        //filling dorm and submit
        find("#username").fill().with("useruser");
        find("#password").fill().with("password");
        find("button").click();
        //should redirect to main page after successful login
        await().atMost(1, TimeUnit.SECONDS);
        isAtMainPage();
    }

    @Test
    public void givenInvalidUsernamePwd_whenSubmit_thenReturnPageWithError(){
        registerAdminUser("useruser","password");
        goTo(loginUrl);
        //filling form with invalid username
        find("#username").fill().with("user");
        find("#password").fill().with("password");
        find("button").click();
        //should reload page and display error
        await().atMost(1, TimeUnit.SECONDS);
        isAtLoginPage();
        assertThat(pageSource()).contains("Неверный логин/пароль");
    }

}
