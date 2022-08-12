package com.app.bank_acquiring.fluentlenium;


import org.fluentlenium.adapter.junit.FluentTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;


@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RegistrationTest extends FluentTest {

    @LocalServerPort
    private Integer port;
    private String registrationUrl;
    private String regPageTitle;

    @Before
    public void setUp(){
        registrationUrl = "http://localhost:" + port + "/accounts/registration";
        regPageTitle = "POS-кассир | Регистрация";
    }

    @Test
    public void testGetAndPost() {
        //opening registration endpoint
        goTo(registrationUrl);
        assertThat(window().title()).contains(regPageTitle);
        //filling register form and click submit button
        find("#username").fill().with("useruser");
        find("#password").fill().with("useruser");
        find("#repeatPWD").fill().with("useruser");
        find("#email").fill().with("user@user.ru");
        find("button").click();
        //should redirect to login page
        await().atMost(1, TimeUnit.SECONDS);
        assertThat(url()).contains("/login.html");
        assertThat(window().title()).contains("POS-кассир | Вход");
    }

    @Test
    public void givenInvalidData_whenPost_thenReturnsErrorsFromServer() {
        //opening registration endpoint
        goTo(registrationUrl);
        assertThat(window().title()).contains(regPageTitle);
        //filling register form with invalid data and click submit button
        find("#username").fill().with("user");//invalid name
        find("#password").fill().with("user");//invalid pwd
        find("#repeatPWD").fill().with("useruser");//pwds not match
        find("#email").fill().with("user@user.ru");
        find("button").click();
        //no redirection to login page due validation errors
        await().atMost(1, TimeUnit.SECONDS);
        assertThat(url()).contains(registrationUrl);
        assertThat(pageSource()).contains("Пароли не совпадают");
        assertThat(pageSource()).contains("Слишком короткий пароль");
        assertThat(pageSource()).contains("Логин от 8 до 40 символов");
    }
}
