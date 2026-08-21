package com.dms.devrytime.domain.auth.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final String from;

    public EmailSender(JavaMailSender javaMailSender,
                       @Value("${app.mail.from}") String from){
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    public void sendVerificationCode(String email, String code){

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[DeveryTime] 이메일 인증 코드");
        message.setText("인증 코드는 "+ code + "입니다. \n" +
                        "해당 인증 코드는 5분동안 유효합니다.");

        javaMailSender.send(message);
    }
}
