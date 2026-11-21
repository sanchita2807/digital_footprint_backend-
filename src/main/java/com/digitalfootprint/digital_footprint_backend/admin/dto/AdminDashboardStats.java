package com.digitalfootprint.digital_footprint_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStats {
    private long totalUsers;
    private long totalAnalyses;
    private long completedAnalyses;
    private long pendingOrFailedAnalyses;
    private long lowRiskCount;
    private long mediumRiskCount;
    private long highRiskCount;
    private long verifiedUsers;
    private long activeUsers;
    private List<TrendPoint> userRegistrationTrend;
    private List<TrendPoint> analysisActivityTrend;
    private List<RiskCategoryCount> riskCategoryDistribution;
    private List<RecentAnalysisDto> recentAnalyses;
}
