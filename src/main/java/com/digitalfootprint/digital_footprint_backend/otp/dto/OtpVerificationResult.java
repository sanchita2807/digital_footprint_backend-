package com.digitalfootprint.digital_footprint_backend.otp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationResult {
    private boolean valid;
    private String message;
    private int remainingAttempts;

    public static OtpVerificationResult success(String message) {
        return OtpVerificationResult.builder()
                .valid(true)
                .message(message)
                .remainingAttempts(0)
                .build();
    }

    public static OtpVerificationResult failure(String message, int remainingAttempts) {
        return OtpVerificationResult.builder()
                .valid(false)
                .message(message)
                .remainingAttempts(remainingAttempts)
                .build();
    }
}
