package com.app.bank_acquiring.unitTest;

import com.app.bank_acquiring.service.EmailServiceComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.*;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailServiceComponentTest {

    @Autowired
    private EmailServiceComponent emailServiceComponent;

    @Test
    public void givenCorrectEmailAddress_whenSendMail_thenThrowNoException() {
        try {
            emailServiceComponent.sendMail("pablo11grande@gmail.com", "cheque");
        } catch (MailException e) {
            fail();
        }
    }

    @Test(expected = MailException.class)
    public void givenIncorrectEmailAddress_whenSendMail_thenThrowException() {
        emailServiceComponent.sendMail("pablo11grandegmail.com", "cheque");
    }
}
