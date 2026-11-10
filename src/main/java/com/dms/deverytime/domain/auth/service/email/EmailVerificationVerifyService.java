package com.dms.deverytime.domain.auth.service.email;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.dto.request.VerifyEmailRequest;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.domain.auth.service.email.validator.EmailVerificationValidator;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationVerifyService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationAttemptService emailVerificationAttemptService;
    private final EmailVerificationValidator emailVerificationValidator;
    private final EmailVerificationSuccessService emailVerificationSuccessService;

    public void verifyEmailCode(VerifyEmailRequest request){

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseThrow(() -> new DeveryTimeException
                                (ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        emailVerificationValidator.validate(verification);
        validateCode(verification, request.code());

        emailVerificationSuccessService.verify(request.email(), request.code());
    }

    private void validateCode(EmailVerification verification, String code){
        if (!verification.getCode().equals(code)) {

            emailVerificationAttemptService.increaseAttemptCount(verification.getId());
            throw new DeveryTimeException(ErrorCode.INVALID_VERIFICATION_CODE);

        }
    }
}