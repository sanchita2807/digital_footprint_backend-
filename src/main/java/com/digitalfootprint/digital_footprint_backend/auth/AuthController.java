package com.digitalfootprint.digital_footprint_backend.auth;

import com.digitalfootprint.digital_footprint_backend.auth.dto.*;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * POST /api/auth/register
     * Initiates registration with full name & phone number.
     * Generates OTP and prints to console in dev mode.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<String> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/send-login-otp
     * Initiates login for an existing registered phone number.
     * Generates OTP and prints to console in dev mode.
     */
    @PostMapping("/send-login-otp")
    public ResponseEntity<ApiResponse<String>> sendLoginOtp(@Valid @RequestBody SendLoginOtpRequest request) {
        ApiResponse<String> response = authService.sendLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/verify-otp
     * Verifies the 6-digit OTP submitted by the user.
     * Returns JWT token and User DTO upon success.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        ApiResponse<AuthResponse> response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/resend-otp
     * Resends a new 6-digit OTP.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        ApiResponse<String> response = authService.resendOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * Authenticated endpoint to test token validity and retrieve current user info.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal String phoneNumber) {
        if (phoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .username(user.getUsername())
                .occupation(user.getOccupation())
                .city(user.getCity())
                .isVerified(user.isVerified())
                .role(user.getIsAdmin() != null && user.getIsAdmin() == 1 ? "Admin" : "User")
                .isAdmin(user.getIsAdmin())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", userDto));
    }
}
