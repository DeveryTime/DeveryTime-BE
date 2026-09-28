package com.dms.deverytime.domain.search.dto.response;

import com.dms.deverytime.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserInfo {
    private final Long id;
    private final String username;
    private final String name;

    public UserInfo(Long id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    //User엔티티에서 필요한 값 3개 뽑아쓰기
    public static UserInfo from(User user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getName());
    }
}
