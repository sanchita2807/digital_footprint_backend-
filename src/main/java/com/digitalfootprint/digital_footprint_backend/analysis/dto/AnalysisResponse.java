package com.digitalfootprint.digital_footprint_backend.analysis.dto;

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
public class AnalysisResponse {
    private Long id;
    private String username;
    private String platform;
    private String profileUrl;
    private Integer overallScore;
    private String riskLevel;
    private Integer totalRisksDetected;
    private List<String> riskExplanation;
    private List<DetectedRiskDto> detectedRisks;
    private List<RecommendationDto> recommendations;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
