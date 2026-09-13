package com.dms.deverytime.domain.user.controller;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.user.service.UserPostQueryService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController {

    private final UserPostQueryService userPostQueryService;

    @GetMapping("/posts")
    public ApiResponse<Page<PostListResponse>> getUserPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ){
        Page<PostListResponse> response =
                userPostQueryService.getUserPosts(userDetails.getUserId(), pageable);

        return ApiResponse.success(response);
    }
}
