package com.dms.deverytime.domain.search.controller;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import com.dms.deverytime.domain.search.dto.request.PostSearchRequest;
import com.dms.deverytime.domain.search.dto.response.PageResponse;
import com.dms.deverytime.domain.search.dto.response.SearchResponse;
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

    // 통합 검색
    @PostMapping
    public ApiResponse<SearchResponse> search(@Valid @RequestBody PostSearchRequest request) {
        return ApiResponse.success(searchService.search(request));
    }
}