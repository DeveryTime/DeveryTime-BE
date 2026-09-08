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

    // 게시글 제목 검색
    @PostMapping("/posts")
    public ApiResponse<PageResponse<PostListResponse>> searchPosts(@Valid @RequestBody PostSearchRequest request) {
        return ApiResponse.success(searchService.searchPostsByTitle(request));
    }
}