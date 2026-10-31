package com.aayush.backend.controller;

import com.aayush.backend.dto.CreateEmergencySessionRequest;
import com.aayush.backend.dto.EmergencySessionResponse;
import com.aayush.backend.dto.UpdateSessionStatusRequest;
import com.aayush.backend.service.EmergencySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Emergency Session operations
 */
@RestController
@RequestMapping("/api/emergency-sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000"})
public class EmergencySessionController {

    private final EmergencySessionService sessionService;

    /**
     * Create a new emergency session
     * 
     * POST /api/emergency-sessions
     */
    @PostMapping
    public ResponseEntity<EmergencySessionResponse> createSession(
            @Valid @RequestBody CreateEmergencySessionRequest request) {
        
        log.info("Received request to create emergency session");
        EmergencySessionResponse response = sessionService.createEmergencySession(request);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all emergency sessions
     * 
     * GET /api/emergency-sessions
     */
    @GetMapping
    public ResponseEntity<List<EmergencySessionResponse>> getAllSessions() {
        log.info("Received request to get all emergency sessions");
        List<EmergencySessionResponse> sessions = sessionService.getAllSessions();
        
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get emergency session by session ID
     * 
     * GET /api/emergency-sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<EmergencySessionResponse> getSessionBySessionId(
            @PathVariable String sessionId) {
        
        log.info("Received request to get session: {}", sessionId);
        EmergencySessionResponse response = sessionService.getSessionBySessionId(sessionId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update emergency session status
     * 
     * PATCH /api/emergency-sessions/{sessionId}/status
     */
    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<EmergencySessionResponse> updateSessionStatus(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateSessionStatusRequest request) {
        
        log.info("Received request to update session {} status to: {}", sessionId, request.getStatus());
        EmergencySessionResponse response = sessionService.updateSessionStatus(sessionId, request);
        
        return ResponseEntity.ok(response);
    }
}
