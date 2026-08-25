package com.dms.deverytime.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "리프레시 토큰을 전달해주세요.")
        String refreshToken
) {
}
