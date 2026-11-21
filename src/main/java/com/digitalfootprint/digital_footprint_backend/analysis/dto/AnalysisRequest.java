package com.digitalfootprint.digital_footprint_backend.analysis.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnalysisRequest {

    @Size(max = 100, message = "Username must not exceed 100 characters")
    private String username;

    @Size(max = 80, message = "Platform name must not exceed 80 characters")
    private String platform;

    @Pattern(
        regexp = "^$|^(https?://).{1,490}$",
        message = "Profile URL must be a valid HTTP/HTTPS URL"
    )
    @Size(max = 500, message = "Profile URL must not exceed 500 characters")
    private String profileUrl;

    @Size(max = 10000, message = "Content must not exceed 10,000 characters")
    private String content;
}
