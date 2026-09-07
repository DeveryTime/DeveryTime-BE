package com.dms.deverytime.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @Pattern(regexp = "\\d{4}", message = "자신의 현재 4자리 학번을 입력해주세요.")
        @NotBlank(message = "학번은 필수 입력 항목입니다.")
        String schoolNumber,

        @Pattern(regexp = "^[가-힣]+$", message = "이름은 한글로 입력해주세요.")
        @Size(max = 30, message = "이름은 30자 이하로 입력해주세요.")
        @NotBlank(message = "이름은 필수 입력 항목입니다.")
        String name,

        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        @Pattern(regexp = "^.+@dsm\\.hs\\.kr$",
                message = "학교 이메일(@dsm.hs.kr)만 사용하여 회원가입할 수 있습니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요.")
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        String email,

        @Size(max = 10, message = "아이디는 10자 이하로 입력해주세요.")
        @NotBlank(message = "아이디는 필수 입력 항목입니다.")
        String username,

        @Size(min = 8, max = 20, message = "비밀번호는 8~20자리여야 합니다.")
        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        String password,

        @NotBlank(message = "비밀번호를 다시 입력해주세요.")
        String passwordConfirm
) {
}
