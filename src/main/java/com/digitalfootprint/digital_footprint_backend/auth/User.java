package com.digitalfootprint.digital_footprint_backend.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_phone", columnList = "phone_number", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "occupation", length = 80)
    private String occupation;

    @Column(name = "city", length = 80)
    private String city;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    /** BCrypt-hashed password — set only for admin accounts, null for OTP-based users. */
    @Column(name = "password", length = 100)
    private String password;

    /** Account active flag — admins can deactivate users. Defaults to true. */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Role indicator: 0 = Normal User, 1 = Administrator.
     * This field provides a simple integer representation of user roles.
     * Defaults to 0 (normal user).
     */
    @Builder.Default
    @Column(name = "is_admin", nullable = false)
    private Integer isAdmin = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
