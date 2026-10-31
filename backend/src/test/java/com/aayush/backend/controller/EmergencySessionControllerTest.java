package com.aayush.backend.controller;

import com.aayush.backend.dto.CreateEmergencySessionRequest;
import com.aayush.backend.dto.PatientProfileDTO;
import com.aayush.backend.dto.UpdateSessionStatusRequest;
import com.aayush.backend.repository.EmergencySessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmergencySessionController
 * Uses Docker MongoDB container at localhost:27017 with dedicated test database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmergencySessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmergencySessionRepository repository;

    @AfterEach
    void cleanUp() {
        // Clean up test data after each test to ensure isolation
        repository.deleteAll();
    }

    @Test
    void testCreateEmergencySession_Success() throws Exception {
        // Given
        PatientProfileDTO patient = PatientProfileDTO.builder()
                .name("Test Patient")
                .age(75)
                .allergies("Penicillin")
                .emergencyContact("+91-9876543210")
                .build();

        CreateEmergencySessionRequest request = CreateEmergencySessionRequest.builder()
                .patientProfile(patient)
                .symptoms("I have severe chest pain and difficulty breathing")
                .language("en")
                .build();

        // When/Then
        mockMvc.perform(post("/api/emergency-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.patientProfile.name").value("Test Patient"))
                .andExpect(jsonPath("$.patientProfile.age").value(75))
                .andExpect(jsonPath("$.symptoms").exists())
                .andExpect(jsonPath("$.triageAssessment.urgencyLevel").exists())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void testCreateEmergencySession_ValidationError_MissingName() throws Exception {
        // Given
        PatientProfileDTO patient = PatientProfileDTO.builder()
                .age(75)
                .emergencyContact("+91-9876543210")
                .build();

        CreateEmergencySessionRequest request = CreateEmergencySessionRequest.builder()
                .patientProfile(patient)
                .symptoms("I have chest pain")
                .language("en")
                .build();

        // When/Then
        mockMvc.perform(post("/api/emergency-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testCreateEmergencySession_ValidationError_InvalidAge() throws Exception {
        // Given
        PatientProfileDTO patient = PatientProfileDTO.builder()
                .name("Test Patient")
                .age(0)  // Invalid age
                .emergencyContact("+91-9876543210")
                .build();

        CreateEmergencySessionRequest request = CreateEmergencySessionRequest.builder()
                .patientProfile(patient)
                .symptoms("I have chest pain")
                .language("en")
                .build();

        // When/Then
        mockMvc.perform(post("/api/emergency-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllSessions() throws Exception {
        mockMvc.perform(get("/api/emergency-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetSessionBySessionId_NotFound() throws Exception {
        mockMvc.perform(get("/api/emergency-sessions/NON_EXISTENT_ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testUpdateSessionStatus_Success() throws Exception {
        // First create a session
        PatientProfileDTO patient = PatientProfileDTO.builder()
                .name("Test Patient")
                .age(70)
                .emergencyContact("+91-1234567890")
                .build();

        CreateEmergencySessionRequest createRequest = CreateEmergencySessionRequest.builder()
                .patientProfile(patient)
                .symptoms("Fever and cough")
                .language("en")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/emergency-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(responseBody).get("sessionId").asText();

        // Now update the status
        UpdateSessionStatusRequest updateRequest = UpdateSessionStatusRequest.builder()
                .status("ACKNOWLEDGED")
                .build();

        mockMvc.perform(patch("/api/emergency-sessions/" + sessionId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.sessionId").value(sessionId));
    }

    @Test
    void testUpdateSessionStatus_InvalidStatus() throws Exception {
        UpdateSessionStatusRequest request = UpdateSessionStatusRequest.builder()
                .status("INVALID_STATUS")
                .build();

        mockMvc.perform(patch("/api/emergency-sessions/SOME_ID/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
