package com.aayush.backend.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenResponse
 * 
 * Verify correct serialization and field mapping for token refresh operations
 */
class TokenResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testTokenResponse_AllFields() {
        // When
        TokenResponse response = TokenResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newAccess")
                .refreshToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newRefresh")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newAccess", response.getAccessToken());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newRefresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900, response.getExpiresIn());
    }

    @Test
    void testTokenResponse_Serialization() throws JsonProcessingException {
        // Given
        TokenResponse response = TokenResponse.builder()
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .tokenType("Bearer")
                .expiresIn(1800)
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertTrue(json.contains("accessToken"));
        assertTrue(json.contains("refreshToken"));
        assertTrue(json.contains("tokenType"));
        assertTrue(json.contains("Bearer"));
        assertTrue(json.contains("expiresIn"));
        assertTrue(json.contains("1800"));
        assertTrue(json.contains("new.access.token"));
        assertTrue(json.contains("new.refresh.token"));
    }

    @Test
    void testTokenResponse_Deserialization() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "accessToken": "deserialized.access.token",
                    "refreshToken": "deserialized.refresh.token",
                    "tokenType": "Bearer",
                    "expiresIn": 3600
                }
                """;

        // When
        TokenResponse response = objectMapper.readValue(json, TokenResponse.class);

        // Then
        assertEquals("deserialized.access.token", response.getAccessToken());
        assertEquals("deserialized.refresh.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
    }

    @Test
    void testTokenResponse_NoArgsConstructor() {
        // When
        TokenResponse response = new TokenResponse();
        response.setAccessToken("test.access");
        response.setRefreshToken("test.refresh");
        response.setTokenType("Bearer");
        response.setExpiresIn(600);

        // Then
        assertEquals("test.access", response.getAccessToken());
        assertEquals("test.refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(600, response.getExpiresIn());
    }

    @Test
    void testTokenResponse_AllArgsConstructor() {
        // When
        TokenResponse response = new TokenResponse(
                "constructor.access",
                "constructor.refresh",
                "Bearer",
                1200
        );

        // Then
        assertEquals("constructor.access", response.getAccessToken());
        assertEquals("constructor.refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1200, response.getExpiresIn());
    }

    @Test
    void testTokenResponse_Builder() {
        // When
        TokenResponse response = TokenResponse.builder()
                .accessToken("builder.access")
                .refreshToken("builder.refresh")
                .tokenType("Bearer")
                .expiresIn(2400)
                .build();

        // Then
        assertEquals("builder.access", response.getAccessToken());
        assertEquals("builder.refresh", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(2400, response.getExpiresIn());
    }

    @Test
    void testTokenResponse_EqualsAndHashCode() {
        // Given
        TokenResponse response1 = TokenResponse.builder()
                .accessToken("same.access")
                .refreshToken("same.refresh")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        TokenResponse response2 = TokenResponse.builder()
                .accessToken("same.access")
                .refreshToken("same.refresh")
                .tokenType("Bearer")
                .expiresIn(900)
                .build();

        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void testTokenResponse_ToString() {
        // Given
        TokenResponse response = TokenResponse.builder()
                .accessToken("string.access")
                .refreshToken("string.refresh")
                .tokenType("Bearer")
                .expiresIn(1500)
                .build();

        // When
        String toString = response.toString();

        // Then
        assertTrue(toString.contains("string.access"));
        assertTrue(toString.contains("string.refresh"));
        assertTrue(toString.contains("Bearer"));
        assertTrue(toString.contains("1500"));
    }

    @Test
    void testTokenResponse_WithLongToken() {
        // Given - simulate realistic JWT tokens
        String longAccessToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6IlNFTklPUiIsImlhdCI6MTcwNDEwMjQwMCwiZXhwIjoxNzA0MTA2MDAwfQ.signature_hash_here_very_long_string_to_simulate_real_jwt";
        String longRefreshToken = "eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbkZhbWlseUlkIjoiZmFtaWx5MTIzIiwidXNlcklkIjoidXNlcjEyMyIsImlhdCI6MTcwNDEwMjQwMCwiZXhwIjoxNzA0MTg4ODAwfQ.another_signature_hash_here_very_long_string";

        // When
        TokenResponse response = TokenResponse.builder()
                .accessToken(longAccessToken)
                .refreshToken(longRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

        // Then
        assertEquals(longAccessToken, response.getAccessToken());
        assertEquals(longRefreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
    }
}
