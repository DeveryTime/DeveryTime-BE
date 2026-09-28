package com.dms.deverytime.domain.search.dto.response;

import com.dms.deverytime.domain.post.dto.response.PostListResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class SearchResponse {

    private final List<UserInfo> users;
    private final PageResponse<PostListResponse> posts;

    public SearchResponse(List<UserInfo> users, PageResponse<PostListResponse> posts) {
        this.users = users;
        this.posts = posts;
    }

}
