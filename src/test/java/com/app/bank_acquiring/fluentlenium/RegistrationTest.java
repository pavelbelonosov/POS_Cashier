package com.app.bank_acquiring.fluentlenium;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


import java.util.concurrent.TimeUnit;

import static org.fluentlenium.assertj.FluentLeniumAssertions.assertThat;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class RegistrationTest extends BaseTest {

    @Test
    public void whenSubmit_thenRedirectToLoginPage() {
        //opening registration endpoint
        goTo(registrationUrl);
        isAtRegistrationPage();
        //filling register form and click submit button
        find("#username").fill().with("useruser");
        find("#password").fill().with("useruser");
        find("#repeatPWD").fill().with("useruser");
        find("#email").fill().with("user@user.ru");
        find("button").click();
        //should redirect to login page
        await().atMost(1, TimeUnit.SECONDS);
        assertThat(url()).contains(loginUrl);
        isAtLoginPage();
    }

    @Test
    public void givenInvalidData_whenSubmit_thenReloadPageWithErrors() {
        //opening registration endpoint
        goTo(registrationUrl);
        isAtRegistrationPage();
        //filling register form with invalid data and click submit button
        find("#username").fill().with("useruser");
        find("#password").fill().with("password");
        find("#repeatPWD").fill().with("differentPassword");//pwds not match
        find("#email").fill().with("user@user.ru");
        find("button").click();
        //no redirection to login page due validation error
        await().atMost(1, TimeUnit.SECONDS);
        assertThat(url()).contains(registrationUrl);
        assertThat(pageSource()).contains("Пароли не совпадают");
    }
}
