package com.digitalfootprint.digital_footprint_backend.security;

import com.digitalfootprint.digital_footprint_backend.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility class to prevent unauthorized role changes.
 * This ensures that users cannot escalate their own privileges.
 */
@Slf4j
public class RoleProtectionUtil {

    /**
     * Prevents users from changing their own role/admin status during profile updates.
     * If an update attempts to modify isAdmin or role, this method logs a warning and throws an exception.
     *
     * @param originalUser The user from the database (current state)
     * @param updatedUser  The user object with attempted changes
     * @throws ResponseStatusException if role/isAdmin change is detected
     */
    public static void preventRoleEscalation(User originalUser, User updatedUser) {
        // Check if isAdmin was attempted to be changed
        if (originalUser.getIsAdmin() != null && updatedUser.getIsAdmin() != null) {
            if (!originalUser.getIsAdmin().equals(updatedUser.getIsAdmin())) {
                log.warn("User {} attempted to change isAdmin from {} to {}", 
                    originalUser.getId(), 
                    originalUser.getIsAdmin(), 
                    updatedUser.getIsAdmin()
                );
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User cannot modify their own role"
                );
            }
        }
    }

    /**
     * Validates that a user has admin privileges.
     *
     * @param user The user to validate
     * @return true if user is admin
     * @throws ResponseStatusException if user is not admin
     */
    public static boolean validateAdminAccess(User user) {
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Admin access required"
            );
        }
        
        if (!user.isActive()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "User account is inactive"
            );
        }
        
        return true;
    }

    /**
     * Validates that a user is NOT an admin (for user-only operations).
     *
     * @param user The user to validate
     * @return true if user is not admin
     */
    public static boolean validateUserNotAdmin(User user) {
        if (user != null && user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            log.warn("Admin user {} attempted to perform user-only operation", user.getId());
            return false;
        }
        return true;
    }
}
