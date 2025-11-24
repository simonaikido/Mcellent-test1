package com.bim.seif.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Configuration
//@Profile("prod")
public class DataInitializer {
//
//    @Bean
//    public JavaMailSender javaMailSender() throws NamingException {
//
//        InitialContext ctx = new InitialContext();
//        Session session = (Session) ctx.lookup("mail/MailSessionSeif");
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setSession(session);
//        return mailSender;
//    }


}