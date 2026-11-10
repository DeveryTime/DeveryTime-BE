package com.dms.deverytime.domain.auth.service.email;

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
    private static final int MAX_ATTEMPTS = 5;

    public int increaseAttemptCount(Long verificationId){

        int updatedRows = verificationRepository
                .increaseAttemptCount(verificationId, MAX_ATTEMPTS);

        if (updatedRows == 0)
            throw new DeveryTimeException(ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED);

        return updatedRows;
    }
}
