package com.digitalfootprint.digital_footprint_backend.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Please provide a valid E.164 phone number (e.g. +919876543210)")
    private String phoneNumber;

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\.]+$", message = "Username can only contain letters, numbers, underscores, or dots")
    private String username;

    @Size(max = 80, message = "Occupation must not exceed 80 characters")
    private String occupation;

    @Size(max = 80, message = "City must not exceed 80 characters")
    private String city;
}
