package com.digitalfootprint.digital_footprint_backend.auth;

import com.digitalfootprint.digital_footprint_backend.auth.dto.*;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import com.digitalfootprint.digital_footprint_backend.otp.OtpPurpose;
import com.digitalfootprint.digital_footprint_backend.otp.OtpService;
import com.digitalfootprint.digital_footprint_backend.otp.dto.OtpVerificationResult;
import com.digitalfootprint.digital_footprint_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Step 1 of Registration: Registers user as unverified and sends OTP.
     */
    @Transactional
    public ApiResponse<String> register(RegisterRequest request) {
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        var existingUserOpt = userRepository.findByPhoneNumber(normalizedPhone);
        if (existingUserOpt.isPresent() && existingUserOpt.get().isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already registered. Please log in.");
        }

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            user.setFullName(request.getFullName().trim());
            if (request.getEmail() != null) user.setEmail(request.getEmail().trim().toLowerCase());
            if (request.getUsername() != null) user.setUsername(request.getUsername().trim());
            if (request.getOccupation() != null) user.setOccupation(request.getOccupation().trim());
            if (request.getCity() != null) user.setCity(request.getCity().trim());
        } else {
            user = User.builder()
                    .fullName(request.getFullName().trim())
                    .phoneNumber(normalizedPhone)
                    .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                    .username(request.getUsername() != null ? request.getUsername().trim() : null)
                    .occupation(request.getOccupation() != null ? request.getOccupation().trim() : null)
                    .city(request.getCity() != null ? request.getCity().trim() : null)
                    .isVerified(false)
                    .build();
        }
        userRepository.save(user);

        // Generate OTP and trigger OTP service (prints to console in dev)
        otpService.generateAndSendOtp(normalizedPhone, OtpPurpose.REGISTRATION);

        return ApiResponse.success(
                "OTP sent successfully to " + maskPhoneNumber(normalizedPhone),
                maskPhoneNumber(normalizedPhone)
        );
    }

    /**
     * Step 1 of Login: Validates phone number and sends OTP.
     */
    @Transactional
    public ApiResponse<String> sendLoginOtp(SendLoginOtpRequest request) {
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        var userOpt = userRepository.findByPhoneNumber(normalizedPhone);
        if (userOpt.isEmpty() || !userOpt.get().isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No account found with this phone number. Please create an account."
            );
        }

        // Generate OTP and trigger OTP service (prints to console in dev)
        otpService.generateAndSendOtp(normalizedPhone, OtpPurpose.LOGIN);

        return ApiResponse.success(
                "OTP sent successfully to " + maskPhoneNumber(normalizedPhone),
                maskPhoneNumber(normalizedPhone)
        );
    }

    /**
     * Step 2 of Login / Registration: Verifies OTP and returns auth session token.
     */
    @Transactional
    public ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request) {
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        OtpVerificationResult verification = otpService.verifyOtp(normalizedPhone, request.getOtp());
        if (!verification.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, verification.getMessage());
        }

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found. Please start registration again."
                ));

        boolean wasNew = !user.isVerified();
        if (wasNew) {
            user.setVerified(true);
            userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user.getPhoneNumber(), user.getId(), user.getIsAdmin());

        String displayRole = (user.getIsAdmin() != null && user.getIsAdmin() == 1) ? "Admin" : "User";

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .username(user.getUsername())
                .occupation(user.getOccupation())
                .city(user.getCity())
                .isVerified(user.isVerified())
                .role(displayRole)
                .isAdmin(user.getIsAdmin())
                .createdAt(user.getCreatedAt())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userDto)
                .isNewUser(wasNew)
                .message(wasNew ? "Account created and verified successfully." : "Login successful.")
                .build();

        return ApiResponse.success(authResponse.getMessage(), authResponse);
    }

    /**
     * Resends an OTP for an active login or registration session.
     */
    @Transactional
    public ApiResponse<String> resendOtp(ResendOtpRequest request) {
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No ongoing session found for this phone number. Please submit your details again."
                ));

        OtpPurpose purpose = user.isVerified() ? OtpPurpose.LOGIN : OtpPurpose.REGISTRATION;
        otpService.resendOtp(normalizedPhone, purpose);

        return ApiResponse.success(
                "New OTP sent successfully to " + maskPhoneNumber(normalizedPhone),
                maskPhoneNumber(normalizedPhone)
        );
    }

    /**
     * Helper to normalize phone numbers (e.g. strips spaces and hyphens).
     * Automatically adds +91 prefix if not present (for Indian phone numbers).
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        // Remove spaces, hyphens, and any other non-digit characters except +
        String normalized = phone.replaceAll("[\\s-]", "").trim();
        
        // If phone doesn't start with +, add +91 (Indian country code)
        if (!normalized.startsWith("+")) {
            // If it starts with 91, add +
            if (normalized.startsWith("91")) {
                normalized = "+" + normalized;
            } else {
                // Otherwise add +91
                normalized = "+91" + normalized;
            }
        }
        
        return normalized;
    }

    /**
     * Masks phone number for safe display (e.g. +91 98******45).
     */
    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        String normalized = normalizePhoneNumber(phone);
        int len = normalized.length();
        int prefixEnd = Math.min(len - 4, 5);
        return normalized.substring(0, prefixEnd) + "******" + normalized.substring(len - 2);
    }
}
