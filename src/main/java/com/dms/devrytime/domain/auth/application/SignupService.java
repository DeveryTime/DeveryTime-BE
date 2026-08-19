package com.dms.devrytime.domain.auth.application;

import com.dms.devrytime.domain.auth.domain.emailverification.EmailVerification;
import com.dms.devrytime.domain.auth.domain.emailverification.EmailVerificationRepository;
import com.dms.devrytime.domain.auth.presentation.dto.request.SignupRequest;
import com.dms.devrytime.domain.user.domain.User;
import com.dms.devrytime.domain.user.domain.UserRepository;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            throw new DevryTimeException(ErrorCode.EMAIL_ALREADY_EXISTS);

        if (userRepository.existsByUsername(request.username()))
            throw new DevryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);

        if (!request.password().equals(request.passwordConfirm()))
            throw new DevryTimeException(ErrorCode.PASSWORD_MISMATCH);

        int schoolYear = LocalDate.now().getYear();

        if (userRepository.existsBySchoolNumberAndSchoolYear(request.schoolNumber(), schoolYear))
            throw new DevryTimeException(ErrorCode.SCHOOL_NUMBER_ALREADY_EXISTS);

        EmailVerification verification =
                emailVerificationRepository.findByEmail(request.email())
                        .orElseThrow(() -> new DevryTimeException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!verification.isVerified())
            throw new DevryTimeException(ErrorCode.EMAIL_NOT_VERIFIED);

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
            throw new DevryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
