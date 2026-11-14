package com.dms.deverytime.domain.post.controller;

import com.dms.deverytime.domain.post.dto.request.PostCreateRequest;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.dto.response.PostDetailResponse;
import com.dms.deverytime.domain.post.dto.response.PostImageResponse;
import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.service.PostService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// HTTP로 받고 JSON으로 응답
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 생성
    @PostMapping
    public ApiResponse<Long> createPost(@Valid @RequestBody PostCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        Long postId = postService.createPost(request, loginUserId);
        return ApiResponse.success(postId);
    }

    // 게시글 목록 조회
    @GetMapping
    public ApiResponse<Page<PostListResponse>> getPostList(
            @RequestParam(required = false) Long categoryId, Pageable pageable) {
        return ApiResponse.success(postService.getPostList(categoryId, pageable));
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public ApiResponse<PostDetailResponse> getPostDetail(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        PostDetailResponse response = postService.getPostDetail(id, loginUserId);
        return ApiResponse.success(response);
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ApiResponse<Void> updatePost(@PathVariable Long id,
                                        @Valid @RequestBody PostUpdateRequest request,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        postService.updatePost(id, request, loginUserId);
        return ApiResponse.successMessage("게시글이 수정되었습니다.");
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        postService.deletePost(id, loginUserId);
        return ApiResponse.successMessage("게시글이 삭제되었습니다.");
    }

    // 게시글 이미지 업로드 — 기존 이미지를 포함해 최대 3장
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<PostImageResponse>> uploadPostImages(
            @PathVariable("id") Long id,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        return ApiResponse.success(postService.uploadPostImages(id, files, loginUserId));
    }

    @PutMapping(value = "/{id}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostImageResponse> replacePostImage(
            @PathVariable("id") Long id,
            @PathVariable("imageId") Long imageId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(postService.replacePostImage(id, imageId, file, userDetails.getUserId()));
    }

}
