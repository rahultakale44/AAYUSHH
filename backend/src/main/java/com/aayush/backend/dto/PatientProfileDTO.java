package com.aayush.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patient Profile Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDTO {

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be less than 150")
    private Integer age;

    @Size(max = 200, message = "Allergies description must not exceed 200 characters")
    private String allergies;

    @NotBlank(message = "Emergency contact is required")
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]+$", message = "Invalid phone number format")
    private String emergencyContact;
}
