package com.digitalfootprint.digital_footprint_backend.otp;

import com.digitalfootprint.digital_footprint_backend.otp.dto.OtpVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.console-print-enabled:true}")
    private boolean consolePrintEnabled;

    /**
     * Generates and stores a new 6-digit OTP for the given phone number and purpose.
     * In development mode, logs the OTP to console.
     */
    @Transactional
    public void generateAndSendOtp(String phoneNumber, OtpPurpose purpose) {
        // Invalidate any previously active OTPs for this phone number
        otpRepository.invalidateAllActiveByPhoneNumber(phoneNumber);

        // Generate 6-digit OTP (100000 - 999999)
        int randomCode = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(randomCode);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusMinutes(expiryMinutes);

        Otp otp = Otp.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiryTime(expiryTime)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .isUsed(false)
                .createdAt(now)
                .build();

        otpRepository.save(otp);

        // Development console printing:
        if (consolePrintEnabled) {
            System.out.println("============================================================");
            System.out.println("OTP for " + phoneNumber + ": " + otpCode);
            System.out.println("Purpose: " + purpose + " | Expires at: " + expiryTime);
            System.out.println("============================================================");
            log.info("OTP generated for phone number {} (purpose: {})", maskPhoneNumber(phoneNumber), purpose);
        }

        // In production, integrate with SMS Provider (e.g. Twilio, AWS SNS, etc.) here.
    }

    /**
     * Verifies the submitted OTP against the active OTP record.
     */
    @Transactional
    public OtpVerificationResult verifyOtp(String phoneNumber, String inputOtp) {
        if (phoneNumber == null || inputOtp == null || inputOtp.trim().length() != 6) {
            return OtpVerificationResult.failure("Please enter a valid 6-digit OTP.", 0);
        }

        String cleanInput = inputOtp.trim();

        var otpOptional = otpRepository.findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(phoneNumber);
        if (otpOptional.isEmpty()) {
            return OtpVerificationResult.failure("No active OTP found. Please request a new OTP.", 0);
        }

        Otp otp = otpOptional.get();

        // Check if expired
        if (otp.isExpired()) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return OtpVerificationResult.failure("OTP has expired. Please request a new OTP.", 0);
        }

        // Check if maximum attempts already exceeded
        if (otp.hasExceededMaxAttempts()) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return OtpVerificationResult.failure("Maximum verification attempts exceeded. Please request a new OTP.", 0);
        }

        // Increment attempt count
        otp.setAttempts(otp.getAttempts() + 1);

        // Check if code matches
        if (!otp.getOtpCode().equals(cleanInput)) {
            int remaining = otp.getMaxAttempts() - otp.getAttempts();
            if (remaining <= 0) {
                otp.setUsed(true);
                otpRepository.save(otp);
                return OtpVerificationResult.failure("Incorrect OTP. Maximum attempts exceeded. Please request a new OTP.", 0);
            }
            otpRepository.save(otp);
            return OtpVerificationResult.failure("Incorrect OTP. " + remaining + " attempt(s) remaining.", remaining);
        }

        // Successful match - invalidate OTP
        otp.setUsed(true);
        otpRepository.save(otp);

        return OtpVerificationResult.success("OTP verified successfully.");
    }

    /**
     * Resends an OTP for the phone number.
     */
    @Transactional
    public void resendOtp(String phoneNumber, OtpPurpose purpose) {
        generateAndSendOtp(phoneNumber, purpose);
    }

    /**
     * Helper to mask phone numbers in logs (e.g. +91 98******45)
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        int len = phone.length();
        int prefixLen = Math.min(len - 4, 5);
        return phone.substring(0, prefixLen) + "******" + phone.substring(len - 2);
    }
}
