package com.cc91.userservice.controller;

import com.cc91.userservice.dto.ApiResponse;
import com.cc91.userservice.dto.ForgotPasswordRequest;
import com.cc91.userservice.dto.LoginRequest;
import com.cc91.userservice.dto.LoginResponse;
import com.cc91.userservice.dto.RefreshTokenRequest;
import com.cc91.userservice.dto.RefreshTokenResponse;
import com.cc91.userservice.dto.RegisterRequest;
import com.cc91.userservice.dto.RegisterResponse;
import com.cc91.userservice.dto.ResetPasswordRequest;
import com.cc91.userservice.dto.VerifyEmailRequest;
import com.cc91.userservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (AuthService.USERNAME_ALREADY_USED.equals(e.getMessage())
                    || AuthService.EMAIL_ALREADY_REGISTERED.equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new RegisterResponse(e.getMessage(), null));
            }
            return ResponseEntity.badRequest().body(new RegisterResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            authService.verifyEmail(request);
            return ResponseEntity.ok(ApiResponse.success("Email verified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            RegisterResponse response = authService.resendVerification(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith(AuthService.ACCOUNT_LOCKED_PREFIX)) {
                return ResponseEntity.status(HttpStatus.LOCKED).body(ApiResponse.error(msg));
            }
            if (msg != null && msg.equals(AuthService.ACCOUNT_BANNED)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(msg));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(msg));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Auth API is running"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            RefreshTokenResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Logged out"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email is registered, a verification code has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
