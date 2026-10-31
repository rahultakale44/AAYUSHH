package com.aayush.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new emergency session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmergencySessionRequest {

    @NotNull(message = "Patient profile is required")
    @Valid
    private PatientProfileDTO patientProfile;

    @NotBlank(message = "Symptoms are required")
    @Size(min = 10, max = 2000, message = "Symptoms must be between 10 and 2000 characters")
    private String symptoms;

    @NotBlank(message = "Language is required")
    @Pattern(regexp = "^(en|hi|mr)$", message = "Language must be en, hi, or mr")
    private String language;
}
