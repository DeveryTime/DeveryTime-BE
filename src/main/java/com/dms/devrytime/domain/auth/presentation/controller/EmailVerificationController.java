package com.dms.devrytime.domain.auth.presentation.controller;

import com.dms.devrytime.domain.auth.application.EmailVerificationSendService;
import com.dms.devrytime.domain.auth.application.EmailVerificationVerifyService;
import com.dms.devrytime.domain.auth.presentation.dto.request.SendEmailVerificationRequest;
import com.dms.devrytime.domain.auth.presentation.dto.request.VerifyEmailRequest;
import com.dms.devrytime.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationSendService emailVerificationSendService;
    private final EmailVerificationVerifyService emailVerificationVerifyService;

    @PostMapping
    public ApiResponse<Void> sendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequest request
    ){
        emailVerificationSendService.sendEmailVerification(request);
        return ApiResponse.successMessage("인증 코드가 발송되었습니다.");
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verifyEmailCode(
            @Valid @RequestBody VerifyEmailRequest request
    ){
        emailVerificationVerifyService.verifyEmailCode(request);
        return ApiResponse.successMessage("이메일이 인증되었습니다.");
    }

}
