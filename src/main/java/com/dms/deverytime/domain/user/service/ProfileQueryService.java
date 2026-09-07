package com.dms.deverytime.domain.user.service;

import com.dms.deverytime.domain.user.dto.response.ProfileDataResponse;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProfileQueryService {

    private final UserRepository userRepository;

    public ProfileDataResponse getProfile(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        return ProfileDataResponse.of(user.getId(), user.getName(), user.getSchoolNumber(),
                user.getEmail(), user.getUsername(), user.getProfileImageUrl());
    }
}
