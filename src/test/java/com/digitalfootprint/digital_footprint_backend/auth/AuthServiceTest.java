package com.digitalfootprint.digital_footprint_backend.auth;

import com.digitalfootprint.digital_footprint_backend.auth.dto.AuthResponse;
import com.digitalfootprint.digital_footprint_backend.auth.dto.RegisterRequest;
import com.digitalfootprint.digital_footprint_backend.auth.dto.SendLoginOtpRequest;
import com.digitalfootprint.digital_footprint_backend.auth.dto.VerifyOtpRequest;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import com.digitalfootprint.digital_footprint_backend.otp.OtpPurpose;
import com.digitalfootprint.digital_footprint_backend.otp.OtpService;
import com.digitalfootprint.digital_footprint_backend.otp.dto.OtpVerificationResult;
import com.digitalfootprint.digital_footprint_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegister_NewUser_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Sanchita Ambekar")
                .phoneNumber("+919876543210")
                .build();

        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.empty());

        ApiResponse<String> response = authService.register(request);

        assertTrue(response.isSuccess());
        verify(userRepository).save(any(User.class));
        verify(otpService).generateAndSendOtp("+919876543210", OtpPurpose.REGISTRATION);
    }

    @Test
    void testRegister_AlreadyVerified_ThrowsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Sanchita Ambekar")
                .phoneNumber("+919876543210")
                .build();

        User verifiedUser = User.builder()
                .id(1L)
                .fullName("Sanchita Ambekar")
                .phoneNumber("+919876543210")
                .isVerified(true)
                .build();

        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(verifiedUser));

        assertThrows(ResponseStatusException.class, () -> authService.register(request));
        verify(otpService, never()).generateAndSendOtp(any(), any());
    }

    @Test
    void testSendLoginOtp_ExistingUser_Success() {
        SendLoginOtpRequest request = SendLoginOtpRequest.builder()
                .phoneNumber("+919876543210")
                .build();

        User user = User.builder()
                .id(1L)
                .fullName("Sanchita Ambekar")
                .phoneNumber("+919876543210")
                .isVerified(true)
                .build();

        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(user));

        ApiResponse<String> response = authService.sendLoginOtp(request);

        assertTrue(response.isSuccess());
        verify(otpService).generateAndSendOtp("+919876543210", OtpPurpose.LOGIN);
    }

    @Test
    void testSendLoginOtp_UnknownUser_ThrowsNotFound() {
        SendLoginOtpRequest request = SendLoginOtpRequest.builder()
                .phoneNumber("+919876543210")
                .build();

        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> authService.sendLoginOtp(request));
    }

    @Test
    void testVerifyOtp_Success() {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .phoneNumber("+919876543210")
                .otp("483921")
                .build();

        User user = User.builder()
                .id(1L)
                .fullName("Sanchita Ambekar")
                .phoneNumber("+919876543210")
                .isVerified(false)
                .isAdmin(0)
                .build();

        when(otpService.verifyOtp("+919876543210", "483921"))
                .thenReturn(OtpVerificationResult.success("OTP verified successfully."));
        when(userRepository.findByPhoneNumber("+919876543210")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken("+919876543210", 1L, 0)).thenReturn("mock-jwt-token");

        ApiResponse<AuthResponse> response = authService.verifyOtp(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("mock-jwt-token", response.getData().getToken());
        assertTrue(response.getData().getUser().isVerified());
        assertTrue(response.getData().isNewUser());
    }
}
