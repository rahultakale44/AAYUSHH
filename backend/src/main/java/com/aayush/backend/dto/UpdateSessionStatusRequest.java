package com.aayush.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating emergency session status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "^(CREATED|ACKNOWLEDGED|TRANSPORT_ASSIGNED|EN_ROUTE|ARRIVED|AT_HOSPITAL|COMPLETED)$",
        message = "Invalid status value"
    )
    private String status;
}
