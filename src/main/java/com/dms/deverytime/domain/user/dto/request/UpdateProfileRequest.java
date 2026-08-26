package com.dms.deverytime.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 10, message = "아이디는 10자 이하로 입력해주세요.")
        String username,
        boolean deleteProfileImage
) {
}
