package com.dms.deverytime.domain.auth.controller;

import com.dms.deverytime.domain.auth.service.LoginService;
import com.dms.deverytime.domain.auth.service.LogoutService;
import com.dms.deverytime.domain.auth.service.ReissueService;
import com.dms.deverytime.domain.auth.service.SignupService;
import com.dms.deverytime.domain.auth.dto.request.LoginRequest;
import com.dms.deverytime.domain.auth.dto.request.LogoutRequest;
import com.dms.deverytime.domain.auth.dto.request.ReissueRequest;
import com.dms.deverytime.domain.auth.dto.request.SignupRequest;
import com.dms.deverytime.domain.auth.dto.response.TokenResponse;
import com.dms.deverytime.global.response.ApiResponse;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;
    private final LoginService loginService;
    private final ReissueService reissueService;
    private final LogoutService logoutService;

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

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request){

        TokenResponse response = loginService.login(request);
        return ApiResponse.success(response, "로그인 되었습니다.");
    }

    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(
            @RequestBody @Valid ReissueRequest request){

        TokenResponse response = reissueService.reissue(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid LogoutRequest request){

        logoutService.logout(request, userDetails.getUserId());
        return ApiResponse.successMessage("로그아웃 되었습니다.");
    }
}
