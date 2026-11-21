package com.digitalfootprint.digital_footprint_backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name cannot exceed 120 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    private String username;
    private String occupation;
    private String city;
    private Integer isAdmin;
    private Boolean isActive;
    private Boolean isVerified;
}
