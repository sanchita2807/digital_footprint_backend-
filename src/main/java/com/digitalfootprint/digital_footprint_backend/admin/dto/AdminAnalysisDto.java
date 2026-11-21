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
public class AdminAnalysisDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String username;
    private String platform;
    private String profileUrl;
    private Integer overallScore;
    private String riskLevel;
    private Integer totalRisksDetected;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
