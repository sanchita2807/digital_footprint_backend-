package com.digitalfootprint.digital_footprint_backend.security;

import java.lang.annotation.*;

/**
 * Annotation to mark methods that require admin privileges.
 * This annotation ensures that only users with isAdmin = 1 can access the method.
 * Should be used on controller methods in the admin API.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresAdmin {
    /**
     * Optional message to return if authorization fails.
     */
    String message() default "Admin access required";
}
