package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class EmailVerificationAttemptService {

    private final EmailVerificationRepository verificationRepository;

    public int increaseAttemptCount(Long verificationId){
        EmailVerification verification =
                verificationRepository.findById(verificationId)
                        .orElseThrow(() -> new DeveryTimeException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        verification.increaseVerificationAttemptCount();
        return verification.getVerificationAttemptCount();
    }
}
