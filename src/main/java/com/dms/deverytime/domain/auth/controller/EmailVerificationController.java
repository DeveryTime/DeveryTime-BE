package com.dms.deverytime.domain.auth.controller;

import com.dms.deverytime.domain.auth.service.EmailVerificationSendService;
import com.dms.deverytime.domain.auth.service.EmailVerificationVerifyService;
import com.dms.deverytime.domain.auth.dto.request.SendEmailVerificationRequest;
import com.dms.deverytime.domain.auth.dto.request.VerifyEmailRequest;
import com.dms.deverytime.global.response.ApiResponse;
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
