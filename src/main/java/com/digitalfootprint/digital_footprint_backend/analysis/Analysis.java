package com.digitalfootprint.digital_footprint_backend.analysis;

import com.digitalfootprint.digital_footprint_backend.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analyses", indexes = {
        @Index(name = "idx_analysis_user", columnList = "user_id"),
        @Index(name = "idx_analysis_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Input fields
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "platform", length = 80)
    private String platform;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // AI Results
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(name = "total_risks_detected", nullable = false)
    private Integer totalRisksDetected;

    @Column(name = "risk_explanation", columnDefinition = "TEXT")
    private String riskExplanation;  // JSON-encoded list of strings

    @Builder.Default
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetectedRisk> detectedRisks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recommendation> recommendations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
