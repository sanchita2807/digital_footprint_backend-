package com.digitalfootprint.digital_footprint_backend.otp;

import com.digitalfootprint.digital_footprint_backend.otp.dto.OtpVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "consolePrintEnabled", true);
    }

    @Test
    void testGenerateOtp_Success() {
        String phone = "+919876543210";
        otpService.generateAndSendOtp(phone, OtpPurpose.LOGIN);

        verify(otpRepository).invalidateAllActiveByPhoneNumber(phone);

        ArgumentCaptor<Otp> captor = ArgumentCaptor.forClass(Otp.class);
        verify(otpRepository).save(captor.capture());

        Otp saved = captor.getValue();
        assertEquals(phone, saved.getPhoneNumber());
        assertEquals(6, saved.getOtpCode().length());
        assertEquals(OtpPurpose.LOGIN, saved.getPurpose());
        assertFalse(saved.isUsed());
        assertEquals(0, saved.getAttempts());
        assertTrue(saved.getExpiryTime().isAfter(LocalDateTime.now()));
    }

    @Test
    void testVerifyOtp_Success() {
        String phone = "+919876543210";
        String code = "123456";

        Otp activeOtp = Otp.builder()
                .id(1L)
                .phoneNumber(phone)
                .otpCode(code)
                .purpose(OtpPurpose.LOGIN)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(3)
                .isUsed(false)
                .build();

        when(otpRepository.findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(phone))
                .thenReturn(Optional.of(activeOtp));

        OtpVerificationResult result = otpService.verifyOtp(phone, code);

        assertTrue(result.isValid());
        assertTrue(activeOtp.isUsed());
        verify(otpRepository).save(activeOtp);
    }

    @Test
    void testVerifyOtp_IncorrectCode_IncrementsAttempts() {
        String phone = "+919876543210";
        String correctCode = "123456";

        Otp activeOtp = Otp.builder()
                .id(1L)
                .phoneNumber(phone)
                .otpCode(correctCode)
                .purpose(OtpPurpose.LOGIN)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(3)
                .isUsed(false)
                .build();

        when(otpRepository.findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(phone))
                .thenReturn(Optional.of(activeOtp));

        OtpVerificationResult result = otpService.verifyOtp(phone, "999999");

        assertFalse(result.isValid());
        assertEquals(1, activeOtp.getAttempts());
        assertFalse(activeOtp.isUsed());
        assertEquals(2, result.getRemainingAttempts());
    }

    @Test
    void testVerifyOtp_Expired() {
        String phone = "+919876543210";

        Otp expiredOtp = Otp.builder()
                .id(1L)
                .phoneNumber(phone)
                .otpCode("123456")
                .purpose(OtpPurpose.LOGIN)
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .attempts(0)
                .maxAttempts(3)
                .isUsed(false)
                .build();

        when(otpRepository.findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(phone))
                .thenReturn(Optional.of(expiredOtp));

        OtpVerificationResult result = otpService.verifyOtp(phone, "123456");

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("expired"));
        assertTrue(expiredOtp.isUsed());
    }
}
