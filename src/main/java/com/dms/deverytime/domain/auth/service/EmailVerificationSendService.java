package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.dto.request.SendEmailVerificationRequest;
import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.infrastructure.EmailSender;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationSendService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    public void sendEmailVerification(SendEmailVerificationRequest request){

        if (userRepository.existsByEmail(request.email()))
            throw new DeveryTimeException(ErrorCode.EMAIL_ALREADY_EXISTS);

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(5);

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseGet(() -> EmailVerification.builder()
                                .email(request.email())
                                .code(code)
                                .expiresAt(expiresAt)
                                .sentAt(now)
                                .requestWindowStartedAt(now)
                                .build());

        if (verification.getId() != null){

            if (verification.isVerified())
                throw new DeveryTimeException(ErrorCode.EMAIL_ALREADY_VERIFIED);

            validateRequestLimit(verification, now);
            verification.update(code, expiresAt, now);

        }

        emailVerificationRepository.save(verification);
        emailSender.sendVerificationCode(verification.getEmail(), verification.getCode());
    }

    private String generateCode(){
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private void validateRequestLimit(EmailVerification verification, LocalDateTime now){
        if (verification.getSentAt().plusMinutes(1).isAfter(now))
            throw new DeveryTimeException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);

        if (verification.getRequestWindowStartedAt().plusMinutes(10).isBefore(now))
            verification.resetRequestWindow(now);

        else {
            if (verification.getRequestCount() >= 5)
                throw new DeveryTimeException(ErrorCode.TOO_MANY_REQUESTS);

            verification.increaseRequestCount();
        }

    }
}
