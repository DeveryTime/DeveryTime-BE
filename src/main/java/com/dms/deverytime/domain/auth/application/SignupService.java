package com.dms.deverytime.domain.auth.application;

import com.dms.deverytime.domain.auth.presentation.dto.request.SignupRequest;
import com.dms.deverytime.domain.user.domain.User;
import com.dms.deverytime.domain.user.domain.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
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

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .username(request.username())
                .schoolNumber(request.schoolNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .schoolYear(schoolYear)
                .build();

        userRepository.save(user);
    }

    public void checkUsername(String username){
        if (userRepository.existsByUsername(username))
            throw new DeveryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
