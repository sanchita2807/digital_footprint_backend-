package com.digitalfootprint.digital_footprint_backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String username;
    private String occupation;
    private String city;
    private boolean isVerified;
    private boolean isActive;
    private String role;
    private long analysisCount;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<RecentAnalysisDto> analysisHistory;
}
