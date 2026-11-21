package com.digitalfootprint.digital_footprint_backend.analysis;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_rec_analysis", columnList = "analysis_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @Column(name = "recommendation_text", columnDefinition = "TEXT", nullable = false)
    private String text;
}
