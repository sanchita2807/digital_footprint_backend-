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
public class AdminRiskMonitoringStats {
    private long totalRiskIndicators;
    private long highRiskIndicators;
    private long mediumRiskIndicators;
    private long lowRiskIndicators;
    private List<RiskCategoryCount> riskCategoryDistribution;
    private List<RiskCategoryCount> highRiskByCategory;
    private List<TrendPoint> riskTrend;
    private List<RiskCategoryCount> mostFrequentCategories;
}
