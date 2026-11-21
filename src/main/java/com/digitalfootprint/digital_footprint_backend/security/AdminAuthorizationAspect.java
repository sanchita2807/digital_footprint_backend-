package com.digitalfootprint.digital_footprint_backend.security;

import com.digitalfootprint.digital_footprint_backend.auth.User;
import com.digitalfootprint.digital_footprint_backend.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * AOP Aspect for enforcing admin authorization.
 * Checks the @RequiresAdmin annotation and verifies that the authenticated user has isAdmin = 1.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorizationAspect {

    private final UserRepository userRepository;

    /**
     * Intercepts methods annotated with @RequiresAdmin.
     * Verifies that the authenticated user is an admin before allowing access.
     */
    @Around("@annotation(requiresAdmin)")
    public Object checkAdminAccess(ProceedingJoinPoint joinPoint, RequiresAdmin requiresAdmin) throws Throwable {
        // Get the authentication principal (phone number) from the request context
        String phoneNumber = getAuthenticatedPhoneNumber(joinPoint);
        
        if (phoneNumber == null) {
            log.warn("Attempt to access admin resource without authentication");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // Look up the user
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    log.warn("Authenticated user not found in database: {}", phoneNumber);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
                });

        // Check if user is admin
        if (user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            log.warn("Non-admin user {} attempted to access admin resource", phoneNumber);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, 
                    requiresAdmin.message()
            );
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Inactive user {} attempted to access admin resource", phoneNumber);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        // User is admin - proceed with the method
        log.debug("Admin user {} authorized to access admin resource", phoneNumber);
        return joinPoint.proceed();
    }

    /**
     * Extracts the authenticated phone number from the method arguments.
     * Looks for an @AuthenticationPrincipal parameter.
     */
    private String getAuthenticatedPhoneNumber(ProceedingJoinPoint joinPoint) {
        // Try to get from SecurityContextHolder
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                return (String) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("Could not extract phone number from SecurityContext", e);
        }

        // This is a fallback - in practice, the JwtAuthenticationFilter should populate SecurityContext
        return null;
    }
}
