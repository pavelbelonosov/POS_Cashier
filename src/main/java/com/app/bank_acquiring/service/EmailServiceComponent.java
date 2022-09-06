package com.app.bank_acquiring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailServiceComponent {

    private JavaMailSender javaMailSender;

    public EmailServiceComponent(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendMail(String sendTo, String subject, String body) throws MailException {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(sendTo);
        mail.setFrom("poscashier2022@gmail.com");
        mail.setSubject(subject);
        mail.setText(body);
        javaMailSender.send(mail);
    }
}
