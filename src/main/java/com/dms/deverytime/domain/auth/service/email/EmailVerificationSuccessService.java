package com.dms.deverytime.domain.auth.service.email;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.domain.auth.service.email.validator.EmailVerificationValidator;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationSuccessService {

    private final EmailVerificationRepository verificationRepository;
    private final EmailVerificationValidator verificationValidator;

    public void verify(String email, String code){
        EmailVerification verification =
                verificationRepository.findByEmailWithLock(email)
                        .orElseThrow(() -> new DeveryTimeException
                                (ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        verificationValidator.validate(verification);
        if (!verification.getCode().equals(code))
            throw new DeveryTimeException(ErrorCode.INVALID_VERIFICATION_CODE);

        verification.verify();
    }
}
