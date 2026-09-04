package com.dms.deverytime.domain.post.controller;

import com.dms.deverytime.domain.post.dto.request.PostRequest;
import com.dms.deverytime.domain.post.dto.request.PostUpdateRequest;
import com.dms.deverytime.domain.post.dto.response.PostDetailResponse;
import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.post.service.PostService;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
// HTTP로 받고 JSON으로 응답
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 생성
    @PostMapping
    public ApiResponse<Long> createPost(@RequestBody PostRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        Long postId = postService.createPost(request, loginUserId);
        return ApiResponse.success(postId);
    }

    // 게시글 목록 조회
    @GetMapping
    public ApiResponse<Page<PostListResponse>> getPostList(Pageable pageable) {
        return ApiResponse.success(postService.getPostList(pageable));
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
                                        @RequestBody PostUpdateRequest request,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long loginUserId = userDetails.getUserId();
        postService.updatePost(id, request, loginUserId);
        return ApiResponse.success("게시글이 수정되었습니다.");
    }
}
