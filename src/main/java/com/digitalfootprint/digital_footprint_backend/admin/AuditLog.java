package com.digitalfootprint.digital_footprint_backend.admin;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records administrative actions for audit and compliance purposes.
 * Never stores passwords, tokens, or sensitive credentials.
 */
@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_audit_admin", columnList = "admin_email"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email of the admin who performed the action. */
    @Column(name = "admin_email", nullable = false, length = 150)
    private String adminEmail;

    /** Type of action: LOGIN, LOGOUT, USER_STATUS_CHANGED, REPORT_GENERATED, etc. */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /** Human-readable description of the action. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Type of entity affected: USER, ANALYSIS, REPORT, RECOMMENDATION, etc. */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** ID of the affected entity, if applicable. */
    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
