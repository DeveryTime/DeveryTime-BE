package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.dto.request.VerifyEmailRequest;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional(noRollbackFor = DeveryTimeException.class)
@RequiredArgsConstructor
public class EmailVerificationVerifyService {

    private final EmailVerificationRepository emailVerificationRepository;

    public void verifyEmailCode(VerifyEmailRequest request){

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseThrow(() -> new DeveryTimeException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        validateVerification(verification);
        validateCode(verification, request.code());

        verification.verify();

    }

    private void validateVerification(EmailVerification verification){

        LocalDateTime now = LocalDateTime.now();

        if (verification.isVerified())
            throw new DeveryTimeException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (!verification.getExpiresAt().isAfter(now))
            throw new DeveryTimeException(ErrorCode.VERIFICATION_CODE_EXPIRED);

        if (verification.getVerificationAttemptCount() >= 5)
            throw new DeveryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);
    }

    private void validateCode(EmailVerification verification, String code){
        if (!verification.getCode().equals(code)) {
            verification.increaseVerificationAttemptCount();

            if (verification.getVerificationAttemptCount() >= 5)
                throw new DeveryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);

            throw new DeveryTimeException(ErrorCode.INVALID_VERIFICATION_CODE);

        }
    }
}