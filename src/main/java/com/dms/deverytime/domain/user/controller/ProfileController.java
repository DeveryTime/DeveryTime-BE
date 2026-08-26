package com.dms.deverytime.domain.user.controller;

import com.dms.deverytime.domain.user.dto.request.UpdateProfileRequest;
import com.dms.deverytime.domain.user.dto.response.ProfileDataResponse;
import com.dms.deverytime.domain.user.service.ProfileQueryService;
import com.dms.deverytime.domain.user.service.ProfileUpdateService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileQueryService profileQueryService;
    private final ProfileUpdateService profileUpdateService;

    @PostMapping
    public ApiResponse<ProfileDataResponse> getProfile
            (@AuthenticationPrincipal  CustomUserDetails userDetails){
        ProfileDataResponse response =
                profileQueryService.getProfile(userDetails.getUserId());

        return ApiResponse.success(response);
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(value = "request") @Valid UpdateProfileRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImg

    ){
        profileUpdateService.updateProfile(userDetails.getUserId(), request, profileImg);
        return ApiResponse.successMessage("변경사항이 저장되었습니다.");
    }
}
