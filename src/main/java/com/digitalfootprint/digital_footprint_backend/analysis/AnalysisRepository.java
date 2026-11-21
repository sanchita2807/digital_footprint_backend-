package com.digitalfootprint.digital_footprint_backend.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /**
     * Find all analyses for a user, ordered by most recent first.
     */
    @Query("SELECT a FROM Analysis a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Analysis> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find a specific analysis that belongs to the given user.
     */
    @Query("SELECT a FROM Analysis a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Analysis> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // ─── Admin queries ───────────────────────────────────────

    long countByRiskLevel(String riskLevel);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = "SELECT a FROM Analysis a LEFT JOIN FETCH a.user ORDER BY a.id ASC",
           countQuery = "SELECT COUNT(a) FROM Analysis a")
    Page<Analysis> findAllWithUsers(Pageable pageable);

    @Query(value = "SELECT a FROM Analysis a LEFT JOIN FETCH a.user " +
           "WHERE (:riskLevel IS NULL OR a.riskLevel = :riskLevel) " +
           "AND (:search IS NULL OR " +
           "  LOWER(a.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           "  LOWER(a.platform) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           "  CAST(a.id AS string) = CAST(:search AS string)) " +
           "ORDER BY a.id ASC",
           countQuery = "SELECT COUNT(a) FROM Analysis a " +
           "WHERE (:riskLevel IS NULL OR a.riskLevel = :riskLevel) " +
           "AND (:search IS NULL OR " +
           "  LOWER(a.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           "  LOWER(a.platform) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           "  CAST(a.id AS string) = CAST(:search AS string))")
    Page<Analysis> searchAnalyses(@Param("search") String search,
                                   @Param("riskLevel") String riskLevel,
                                   Pageable pageable);

    @Query("SELECT a FROM Analysis a LEFT JOIN FETCH a.user WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Analysis> findByUserIdForAdmin(@Param("userId") Long userId);

    @Query(value = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
           "FROM analyses WHERE created_at >= :startDate " +
           "GROUP BY DATE(created_at) ORDER BY dt", nativeQuery = true)
    List<Object[]> countDailyAnalyses(@Param("startDate") java.time.LocalDateTime startDate);
}
