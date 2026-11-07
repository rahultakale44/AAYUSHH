package com.aayush.backend.filter;

import com.aayush.backend.document.AccountStatus;
import com.aayush.backend.document.User;
import com.aayush.backend.document.UserRole;
import com.aayush.backend.repository.UserRepository;
import com.aayush.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * Processes JWT tokens in the Authorization header for each HTTP request.
 * This filter runs once per request before Spring Security's authorization checks.
 * 
 * Flow:
 * 1. Extract Authorization header
 * 2. Check for Bearer token prefix
 * 3. Extract and validate JWT using JwtService
 * 4. Extract user ID and role from JWT claims
 * 5. Load user from database
 * 6. Verify user exists and has ACTIVE status
 * 7. Create Spring Security Authentication object
 * 8. Set authentication in SecurityContextHolder
 * 9. Continue filter chain
 * 
 * Security Notes:
 * - Only ACTIVE users are authenticated
 * - SUSPENDED and DELETED users are rejected
 * - Role is taken from JWT claims (verified by signature)
 * - Invalid/expired/malformed tokens are silently ignored
 * - No database lookup if no Bearer token present
 * - Does not overwrite existing authentication
 * - Never logs raw JWT tokens
 * 
 * @author AAYUSH Team
 * @since Phase 1.3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Skip if already authenticated (avoid redundant processing)
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("Request already authenticated, skipping JWT filter");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract Authorization header
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            // No Authorization header - proceed without authentication
            if (authHeader == null || authHeader.isBlank()) {
                log.debug("No Authorization header present");
                filterChain.doFilter(request, response);
                return;
            }

            // Check for Bearer prefix
            if (!authHeader.startsWith(BEARER_PREFIX)) {
                log.debug("Authorization header does not start with 'Bearer '");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JWT token
            String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
            
            if (jwt.isEmpty()) {
                log.debug("Empty JWT token after Bearer prefix");
                filterChain.doFilter(request, response);
                return;
            }

            // Validate JWT token
            if (!jwtService.validateToken(jwt)) {
                log.debug("JWT validation failed");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user ID from token
            String userId;
            try {
                userId = jwtService.extractUserId(jwt);
            } catch (Exception e) {
                log.debug("Failed to extract user ID from JWT: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract role from token
            UserRole role;
            try {
                role = jwtService.extractRole(jwt);
            } catch (Exception e) {
                log.debug("Failed to extract role from JWT: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Load user from database
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                log.debug("User not found with ID: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Verify user account status is ACTIVE
            if (user.getStatus() != AccountStatus.ACTIVE) {
                log.debug("User account is not active. Status: {}, User ID: {}", 
                    user.getStatus(), userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Verify role consistency (JWT claim vs database)
            // This is a defensive check - the JWT signature already guarantees authenticity
            if (user.getRole() != role) {
                log.warn("Role mismatch detected. JWT role: {}, DB role: {}, User ID: {}", 
                    role, user.getRole(), userId);
                filterChain.doFilter(request, response);
                return;
            }

            // Create Spring Security Authentication object
            // Use role from database (already verified against JWT)
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userId,  // Principal: user ID
                    null,    // Credentials: no password needed (JWT verified)
                    authorities
                );

            // Attach request details
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user: {} with role: {}", userId, user.getRole());

        } catch (Exception e) {
            // Catch any unexpected exceptions to prevent filter chain interruption
            log.error("Unexpected error in JWT authentication filter", e);
            // Continue without authentication
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
