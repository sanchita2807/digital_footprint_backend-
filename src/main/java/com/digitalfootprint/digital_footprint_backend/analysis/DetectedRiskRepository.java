package com.digitalfootprint.digital_footprint_backend.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectedRiskRepository extends JpaRepository<DetectedRisk, Long> {

    long countByRiskLevel(String riskLevel);

    @Query(value = "SELECT d.category, COUNT(*) as cnt FROM detected_risks d " +
           "GROUP BY d.category ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByCategoryNative();

    @Query(value = "SELECT d.category, COUNT(*) as cnt FROM detected_risks d " +
           "WHERE d.risk_level = 'HIGH' GROUP BY d.category ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countHighRiskByCategoryNative();

    @Query(value = "SELECT DATE(a.created_at) as dt, COUNT(*) as cnt " +
           "FROM detected_risks d JOIN analyses a ON d.analysis_id = a.id " +
           "WHERE a.created_at >= :startDate " +
           "GROUP BY DATE(a.created_at) ORDER BY dt", nativeQuery = true)
    List<Object[]> countDailyRisks(@Param("startDate") java.time.LocalDateTime startDate);

    @Query(value = "SELECT DATE(a.created_at) as dt, COUNT(*) as cnt " +
           "FROM detected_risks d JOIN analyses a ON d.analysis_id = a.id " +
           "WHERE a.created_at >= :startDate AND d.risk_level = :riskLevel " +
           "GROUP BY DATE(a.created_at) ORDER BY dt", nativeQuery = true)
    List<Object[]> countDailyRisksByLevel(@Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("riskLevel") String riskLevel);
}
