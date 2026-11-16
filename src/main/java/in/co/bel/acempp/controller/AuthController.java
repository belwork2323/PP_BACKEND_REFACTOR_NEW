package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.auth.*;
import in.co.bel.acempp.dto.auth.resetpswd.ResetPasswordRequest;
import in.co.bel.acempp.dto.auth.resetpswd.ResetPasswordResponse;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.AuthService;
import in.co.bel.acempp.service.CaptchaService;
import in.co.bel.acempp.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@Slf4j
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CaptchaService captchaService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;


    @GetMapping("/generate-captcha")
    public ResponseEntity<AcemResponse<CaptchaResponse>> generateCaptcha() {
        log.info("Captcha generation requested");
        CaptchaResponse data = captchaService.generateCaptcha();
        return AcemTemplate.success(data, "Captcha generated successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AcemResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse data = authService.login(request);

        return AcemTemplate.success(data, "Login successful");
    }
    @PostMapping("/logout")
    public ResponseEntity<AcemResponse<LogoutResponse>> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication) {  // ← JWT filter populates this

        String username = authentication.getName(); // from access token in header
        LogoutResponse data = authService.logout(request, username);
        return AcemTemplate.success(data, "Logout successful");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AcemResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        return AcemTemplate.success(
                authService.refreshToken(request),
                "Token refreshed successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AcemResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        ResetPasswordResponse data = passwordResetService.submitResetRequest(request);
        return AcemTemplate.success(data, "Password reset request submitted successfully");
    }
}
