package com.farmeazy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Mail Configuration for Zoho SMTP
 * 
 * This config only activates when farmeazy.mail.provider=zoho
 * When using Resend (default), this bean is not created.
 */
@Configuration
@ConditionalOnProperty(name = "farmeazy.mail.provider", havingValue = "zoho")
public class MailConfig {

    @Value("${mail.noreply.host}")
    private String noreplyHost;
    @Value("${mail.noreply.port}")
    private int noreplyPort;
    @Value("${mail.noreply.username}")
    private String noreplyUsername;
    @Value("${mail.noreply.password}")
    private String noreplyPassword;

    @Value("${mail.support.host}")
    private String supportHost;
    @Value("${mail.support.port}")
    private int supportPort;
    @Value("${mail.support.username}")
    private String supportUsername;
    @Value("${mail.support.password}")
    private String supportPassword;

    @Value("${mail.info.host}")
    private String infoHost;
    @Value("${mail.info.port}")
    private int infoPort;
    @Value("${mail.info.username}")
    private String infoUsername;
    @Value("${mail.info.password}")
    private String infoPassword;

    @Bean(name = "noReplyMailSender")
    public JavaMailSender noReplyMailSender() {
        return createMailSender(noreplyHost, noreplyPort, noreplyUsername, noreplyPassword);
    }

    @Bean(name = "supportMailSender")
    public JavaMailSender supportMailSender() {
        return createMailSender(supportHost, supportPort, supportUsername, supportPassword);
    }

    @Bean(name = "infoMailSender")
    public JavaMailSender infoMailSender() {
        return createMailSender(infoHost, infoPort, infoUsername, infoPassword);
    }

    private JavaMailSender createMailSender(String host, int port, String username, String password) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        return mailSender;
    }
}
