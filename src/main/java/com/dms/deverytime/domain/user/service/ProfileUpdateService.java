package com.dms.deverytime.domain.user.service;

import com.dms.deverytime.domain.user.dto.request.UpdateProfileRequest;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.cloudinary.dto.ImageUploadResult;
import com.dms.deverytime.global.cloudinary.service.ImageStorageService;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileUpdateService {

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private static final String PROFILE_IMAGE_FOLDER = "profile";

    public void updateProfile(Long userId, UpdateProfileRequest request, MultipartFile profileImg){
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

        if (request.deleteProfileImage()){
            if (user.getProfileImagePublicId() != null)
                imageStorageService.delete(user.getProfileImagePublicId());
            user.profileImgUpdate(null, null);
        }
        else if (profileImg != null && !profileImg.isEmpty()){

            ImageUploadResult result =
                    imageStorageService.upload(profileImg, PROFILE_IMAGE_FOLDER);

            try {
                if (user.getProfileImagePublicId() != null)
                    imageStorageService.delete(user.getProfileImagePublicId());
            } catch (DeveryTimeException e) {
                try {
                    imageStorageService.delete(result.profileImagePublicId());
                } catch (DeveryTimeException cleanupException) {
                    e.addSuppressed(cleanupException);
                }

                throw  e;
            }
            user.profileImgUpdate(result.profileImageUrl(), result.profileImagePublicId());
        }
    }
}
