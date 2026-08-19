package com.dms.devrytime.domain.auth.application;

import com.dms.devrytime.domain.auth.domain.emailverification.EmailVerification;
import com.dms.devrytime.domain.auth.domain.emailverification.EmailVerificationRepository;
import com.dms.devrytime.domain.auth.presentation.dto.request.VerifyEmailRequest;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(noRollbackFor = DevryTimeException.class)
@RequiredArgsConstructor
public class EmailVerificationVerifyService {

    private final EmailVerificationRepository emailVerificationRepository;

    public void verifyEmailCode(VerifyEmailRequest request){

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseThrow(() -> new DevryTimeException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        validateVerification(verification);
        validateCode(verification, request.code());

        verification.verify();

    }

    private void validateVerification(EmailVerification verification){

        LocalDateTime now = LocalDateTime.now();

        if (verification.isVerified())
            throw new DevryTimeException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (verification.getExpiresAt().isBefore(now))
            throw new DevryTimeException(ErrorCode.VERIFICATION_CODE_EXPIRED);

        if (verification.getVerificationAttemptCount() >= 5)
            throw new DevryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);
    }

    private void validateCode(EmailVerification verification, String code){
        if (!verification.getCode().equals(code)) {
            verification.increaseVerificationAttemptCount();

            if (verification.getVerificationAttemptCount() >= 5)
                throw new DevryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);

            throw new DevryTimeException(ErrorCode.INVALID_VERIFICATION_CODE);

        }
    }
}