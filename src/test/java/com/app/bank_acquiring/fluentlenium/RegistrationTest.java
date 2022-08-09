package com.app.bank_acquiring.fluentlenium;

import com.app.bank_acquiring.repository.AccountRepository;
import org.fluentlenium.adapter.junit.FluentTest;

import static org.fluentlenium.core.filter.FilterConstructor.*;
import static org.junit.Assert.*;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.annotation.PageUrl;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AccountRepository accountRepository;

    @LocalServerPort
    private Integer port;

    @Before
    public void setUp(){
    }

    @Test
    public void testGetAndPost(){
       goTo("http://localhost:" + port + "/accounts/registration");
       assertThat(window().title()).contains("POS-кассир | Регистрация");
       //filling reg.form and click submit button
       find("#username").fill().with("useruser");
       find("#password").fill().with("useruser");
       find("#repeatPWD").fill().with("useruser");
       find("#email").fill().with("user@user.ru");
       find("button").click();
       //should redirect to login page
       await().atMost(1, TimeUnit.SECONDS);
       assertThat(url()).contains("/login.html");
       assertThat(window().title()).contains("POS-кассир | Вход");
       //new user is saved
       assertThat(accountRepository.findByUsername("useruser").getUsername().equals("useruser"));
    }
}
