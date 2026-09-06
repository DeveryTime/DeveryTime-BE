package com.dms.deverytime.domain.comment.presentation.controller;

import com.dms.deverytime.domain.comment.application.CommentService;
import com.dms.deverytime.domain.comment.presentation.dto.request.CommentCreateRequest;
import com.dms.deverytime.domain.comment.presentation.dto.request.CommentUpdateRequest;
import com.dms.deverytime.domain.comment.presentation.dto.response.CommentCreateResponse;
import com.dms.deverytime.domain.comment.presentation.dto.response.CommentListResponse;
import com.dms.deverytime.domain.comment.presentation.dto.response.CommentUpdateResponse;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetail,
            @RequestBody @Valid CommentCreateRequest request
    ){
        CommentCreateResponse response = commentService.createComment(postId,userDetail.getUserId(),request);

        return ApiResponse.success(response,"댓글이 등록되었습니다.");

    }

    @GetMapping
    public ApiResponse<List<CommentListResponse>> getComment(
            @PathVariable Long postId
    ){
        List<CommentListResponse> responses = commentService.getList(postId);

        return ApiResponse.success(responses);
    }

    @PatchMapping("/{commentId}")
    public ApiResponse<CommentUpdateResponse> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CommentUpdateRequest request

    ){
        CommentUpdateResponse response = commentService.updateComment(postId,commentId,userDetails.getUserId(),request);
        return ApiResponse.success(response,"댓글이 수정되었습니다.");
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        commentService.deleteComment(postId,commentId,userDetails.getUserId());
        return ApiResponse.successMessage("댓글이 삭제되었습니다.");
    }
}
