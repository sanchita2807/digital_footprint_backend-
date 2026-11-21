package com.digitalfootprint.digital_footprint_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String username;
    private String occupation;
    private String city;
    private boolean isVerified;
    private String role;
    private Integer isAdmin;
    private LocalDateTime createdAt;
}
