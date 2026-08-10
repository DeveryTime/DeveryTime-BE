package com.dms.devrytime.domain.auth.presentation.controller;

import com.dms.devrytime.domain.auth.application.SignupService;
import com.dms.devrytime.domain.auth.presentation.dto.request.SignupRequest;
import com.dms.devrytime.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(
            @RequestBody @Valid SignupRequest request
    ){
        signupService.signup(request);
        return ApiResponse.successMessage("회원가입이 완료되었습니다.");
    }

    @GetMapping("/check-username")
    public ApiResponse<Void> checkUsername(@RequestParam String username){
        signupService.checkUsername(username);
        return ApiResponse.successMessage("사용 가능한 아이디입니다.");
    }
}
