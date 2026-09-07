package com.dms.deverytime.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(

        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        @Pattern(regexp = "^.+@dsm\\.hs\\.kr$",
                message = "학교 이메일(@dsm.hs.kr)만 사용할 수 있습니다.")
        @NotBlank(message = "이메일을 입력해주세요.")
        String email,

        @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        @NotBlank(message = "인증코드를 입력해주세요.")
        String code
) {
}
