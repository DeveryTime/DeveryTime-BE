package com.dms.deverytime.domain.search.dto.response;

import com.dms.deverytime.domain.user.entity.User;

public record UserInfo(
        Long id,
        String username,
        String name,
        String profileImageUrl) {

    //User엔티티에서 필요한 값 뽑아쓰기
    public static UserInfo from(User user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getName(), user.getProfileImageUrl());
    }
}
