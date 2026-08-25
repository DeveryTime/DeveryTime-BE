package com.dms.deverytime.domain.user.controller;

import com.dms.deverytime.domain.user.dto.response.ProfileDataResponse;
import com.dms.deverytime.domain.user.service.ProfileQueryService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileQueryService profileQueryService;

    @PostMapping
    public ApiResponse<ProfileDataResponse> getProfile
            (@AuthenticationPrincipal  CustomUserDetails userDetails){
        ProfileDataResponse response =
                profileQueryService.getProfile(userDetails.getUserId());

        return ApiResponse.success(response);
    }
}
