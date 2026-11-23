package com.dms.deverytime.domain.search.dto.response;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;

import java.util.List;

public record SearchResponse(
        List<UserInfo> users,
        PageResponse<PostSearchInfo> posts) {

}
