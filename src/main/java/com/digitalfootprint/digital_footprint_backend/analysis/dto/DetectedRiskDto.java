package com.digitalfootprint.digital_footprint_backend.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedRiskDto {
    private Long id;
    private String category;
    private String indicator;
    private String riskLevel;
    private String explanation;
    private String impact;
}
