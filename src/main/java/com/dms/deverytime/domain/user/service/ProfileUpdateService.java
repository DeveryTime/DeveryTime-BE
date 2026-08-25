package com.dms.deverytime.domain.user.service;

import com.dms.deverytime.domain.user.dto.request.UpdateProfileRequest;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileUpdateService {

    private final UserRepository userRepository;

    public void updateProfile(Long userId, UpdateProfileRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        boolean usernameChanged =
                request.username() != null &&
                        !user.getUsername().equals(request.username());

        LocalDateTime now = LocalDateTime.now();

        if (usernameChanged){
            if (user.getUsernameUpdatedAt() != null
                    && user.getUsernameUpdatedAt().plusHours(24).isAfter(now))
                throw new DeveryTimeException(ErrorCode.USERNAME_CHANGE_LIMIT_EXCEEDED);

            if (userRepository.existsByUsername(request.username()))
                throw new DeveryTimeException(ErrorCode.USERNAME_ALREADY_EXISTS);

            user.usernameUpdate(request.username());
        }

        if (request.deleteProfileImage())
                user.profileImgUpdate(null);
        else if (request.profileImageUrl() != null)
            user.profileImgUpdate(request.profileImageUrl());
    }
}
