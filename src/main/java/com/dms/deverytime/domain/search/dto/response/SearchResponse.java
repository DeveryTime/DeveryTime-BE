package com.dms.deverytime.domain.search.dto.response;

import java.util.List;

public record SearchResponse(
        List<UserInfo> users,
        PageResponse<PostSearchInfo> posts) {

}
