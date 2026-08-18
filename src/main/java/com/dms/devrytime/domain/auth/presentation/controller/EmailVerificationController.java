package com.dms.devrytime.domain.auth.presentation.controller;

import com.dms.devrytime.domain.auth.application.EmailVerificationService;
import com.dms.devrytime.domain.auth.presentation.dto.request.SendEmailVerificationRequest;
import com.dms.devrytime.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public ApiResponse<Void> sendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequest request
    ){
        emailVerificationService.sendEmailVerification(request);
        return ApiResponse.successMessage("인증 코드가 발송되었습니다.");

    }


}
