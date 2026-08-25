package com.dms.deverytime.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(

        @Size(max = 10, message = "아이디는 10자 이하로 입력해주세요.")
        String username,

        @URL(message = "올바른 URL 형식을 입력해주세요.")
        @Size(max = 500, message = "프로필 사진 URL은 500자 이하여야 합니다.")
        String profileImageUrl,

        boolean deleteProfileImage
) {
}
