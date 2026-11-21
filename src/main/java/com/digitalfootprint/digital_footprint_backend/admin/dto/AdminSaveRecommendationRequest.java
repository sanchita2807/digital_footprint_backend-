package com.digitalfootprint.digital_footprint_backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSaveRecommendationRequest {
    private Long analysisId;

    @NotBlank(message = "Priority is required (HIGH, MEDIUM, LOW)")
    private String priority;

    @NotBlank(message = "Recommendation text is required")
    private String text;
}
