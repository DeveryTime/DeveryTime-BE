package com.dms.deverytime.domain.search.controller;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.search.dto.request.PostSearchRequest;
import com.dms.deverytime.domain.search.dto.response.PageResponse;
import com.dms.deverytime.domain.search.service.SearchService;
import com.dms.deverytime.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 게시글 제목에 포함된 키워드 검색
    @PostMapping("/posts")
    public ApiResponse<PageResponse<PostListResponse>> searchPosts(@Valid @RequestBody PostSearchRequest request) {
        // 서비스 호출 --> 결과를 ApiResponse으로 감싸서 반환
        return ApiResponse.success(searchService.searchPostsByTitle(request));
    }

    // 유저 검색
    @PostMapping("/users")
    public ApiResponse<PageResponse<PostListResponse>> searchPostsByUsername(@Valid @RequestBody PostSearchRequest request) {
        return ApiResponse.success(searchService.searchPostsByUsername(request));
    }
}