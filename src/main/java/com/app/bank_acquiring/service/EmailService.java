package com.app.bank_acquiring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendMail(String sendTo, String cheque) throws MailException {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(sendTo);
        mail.setFrom("poscashier2022@gmail.com");
        mail.setSubject("Чек об операции");
        mail.setText(cheque);
        javaMailSender.send(mail);
    }
}
