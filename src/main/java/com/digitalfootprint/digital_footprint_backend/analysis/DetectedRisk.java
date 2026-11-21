package com.digitalfootprint.digital_footprint_backend.analysis;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detected_risks", indexes = {
        @Index(name = "idx_risk_analysis", columnList = "analysis_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "indicator", nullable = false, length = 150)
    private String indicator;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "impact", columnDefinition = "TEXT")
    private String impact;
}
