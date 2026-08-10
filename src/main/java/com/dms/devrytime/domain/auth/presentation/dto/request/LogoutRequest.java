package com.dms.devrytime.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "리프레시 토큰을 전달해주세요.")
        String refreshToken
) {
}
