package com.digitalfootprint.digital_footprint_backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentAnalysisDto {
    private Long id;
    private String userName;
    private String userEmail;
    private String platform;
    private String username;
    private Integer overallScore;
    private String riskLevel;
    private Integer totalRisksDetected;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
