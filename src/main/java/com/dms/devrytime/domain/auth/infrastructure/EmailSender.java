package com.dms.devrytime.domain.auth.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;

    public void sendVerificationCode(String email, String code){

        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(email);
        message.setSubject("[DevryTime] 이메일 인증 코드");
        message.setText("인증 코드는 "+ code + "입니다. \n" +
                        "해당 인증 코드는 5분동안 유효합니다.");

        javaMailSender.send(message);
    }
}
