package com.digitalfootprint.digital_footprint_backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "isActive status is required")
    private Boolean isActive;
}
