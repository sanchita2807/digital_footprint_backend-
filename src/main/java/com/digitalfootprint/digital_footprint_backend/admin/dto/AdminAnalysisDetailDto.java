package com.digitalfootprint.digital_footprint_backend.admin.dto;

import com.digitalfootprint.digital_footprint_backend.analysis.dto.DetectedRiskDto;
import com.digitalfootprint.digital_footprint_backend.analysis.dto.RecommendationDto;
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
public class AdminAnalysisDetailDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String username;
    private String platform;
    private String profileUrl;
    private String content;
    private Integer overallScore;
    private String riskLevel;
    private Integer totalRisksDetected;
    private List<String> riskExplanation;
    private List<DetectedRiskDto> detectedRisks;
    private List<RecommendationDto> recommendations;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
