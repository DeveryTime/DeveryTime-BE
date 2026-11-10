package com.dms.deverytime.domain.auth.service;

import com.dms.deverytime.domain.auth.dto.request.SignupRequest;
import com.dms.deverytime.domain.auth.entity.EmailVerification;
import com.dms.deverytime.domain.auth.repository.EmailVerificationRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;

    public void signup(SignupRequest request){
        if(userRepository.existsByEmail(request.email()))
            throw new DeveryTimeException(ErrorCode.EMAIL_ALREADY_EXISTS);

        if (userRepository.existsByUsername(request.username()))
            throw new DeveryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);

        if (!request.password().equals(request.passwordConfirm()))
            throw new DeveryTimeException(ErrorCode.PASSWORD_MISMATCH);

        int schoolYear = LocalDate.now().getYear();

        if (userRepository.existsBySchoolNumberAndSchoolYear(request.schoolNumber(), schoolYear))
            throw new DeveryTimeException(ErrorCode.SCHOOL_NUMBER_ALREADY_EXISTS);

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseThrow(() -> new DeveryTimeException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!verification.isVerified())
            throw new DeveryTimeException(ErrorCode.EMAIL_NOT_VERIFIED);

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .username(request.username())
                .schoolNumber(request.schoolNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .schoolYear(schoolYear)
                .build();

        userRepository.save(user);
        emailVerificationRepository.delete(verification);
    }

    public void checkUsername(String username){
        if (userRepository.existsByUsername(username))
            throw new DeveryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
