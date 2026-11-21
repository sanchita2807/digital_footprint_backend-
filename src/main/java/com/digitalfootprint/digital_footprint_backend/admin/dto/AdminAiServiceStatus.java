package com.digitalfootprint.digital_footprint_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAiServiceStatus {
    private boolean serviceOnline;
    private String serviceUrl;
    private long totalAnalysesProcessed;
    private long successfulAnalyses;
    private long highRiskAnalyses;
    private long mediumRiskAnalyses;
    private long lowRiskAnalyses;
    private long recentAnalysesCount;
}
