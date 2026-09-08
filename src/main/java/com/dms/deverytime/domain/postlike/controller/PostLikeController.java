package com.dms.deverytime.domain.postlike.controller;


import com.dms.deverytime.domain.postlike.dto.response.PostLikeResponse;
import com.dms.deverytime.domain.postlike.service.PostLikeService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostLikeResponse> createLike (@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        PostLikeResponse response = postLikeService.createLike(postId, loginUserId);
        return ApiResponse.success(response,"좋아요가 등록되었습니다.");

    }

    @DeleteMapping
    public ApiResponse<PostLikeResponse> cancelLike (@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails){
        PostLikeResponse response =postLikeService.cancelLike(postId,userDetails.getUserId());
        return ApiResponse.success(response,"좋아요가 취소되었습니다.");
    }
}
