package com.app.bank_acquiring.unit;

import com.app.bank_acquiring.service.EmailServiceComponent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.mockito.Mockito;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ActiveProfiles("test")
public class EmailServiceComponentTest {

    private JavaMailSender javaMailSender = Mockito.mock(JavaMailSender.class);
    private EmailServiceComponent emailServiceComponent;

    @Before
    public void setUp() {
        emailServiceComponent = new EmailServiceComponent(javaMailSender);
    }

    @Test
    public void whenSendMail_thenSetAddressAndMessageTextRight() {
        ArgumentCaptor<SimpleMailMessage> valueCapture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(javaMailSender).send(valueCapture.capture());
        emailServiceComponent.sendMail("fooMail@gmail.com","Чек об операции", "cheque");

        assertTrue(valueCapture.getValue().getTo()[0].equals("fooMail@gmail.com"));
        assertTrue(valueCapture.getValue().getText().equals("cheque"));
    }

    @Test(expected = Exception.class)
    public void givenIncorrectEmailAddress_whenSendMail_thenThrowException() {
        doThrow(Exception.class).when(javaMailSender).send(any(SimpleMailMessage.class));
        emailServiceComponent.sendMail("pablo11grandegmail.com","Чек об операции", "cheque");
    }
}
