package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class EmailVerificationCreateService {

    private final EmailVerificationRepository verificationRepository;

    public EmailVerification create(EmailVerification verification){
        return verificationRepository.saveAndFlush(verification);
    }


}
