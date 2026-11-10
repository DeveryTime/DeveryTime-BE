package com.dms.deverytime.domain.auth.service.email.validator;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class EmailVerificationValidator {

    private static final int MAX_ATTEMPTS = 5;

    public void validate(EmailVerification verification){

        LocalDateTime now = LocalDateTime.now();

        if (verification.isVerified())
            throw new DeveryTimeException(ErrorCode.EMAIL_ALREADY_VERIFIED);

        if (!verification.getExpiresAt().isAfter(now))
            throw new DeveryTimeException(ErrorCode.VERIFICATION_CODE_EXPIRED);

        if (verification.getVerificationAttemptCount() >= 5)
            throw new DeveryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);
    }
}
