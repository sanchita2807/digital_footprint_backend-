package com.digitalfootprint.digital_footprint_backend.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    @Query(value = "SELECT r FROM Recommendation r JOIN FETCH r.analysis a JOIN FETCH a.user u " +
           "WHERE (:priority IS NULL OR r.priority = :priority) " +
           "AND (:search IS NULL OR LOWER(r.text) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Recommendation r JOIN r.analysis a " +
           "WHERE (:priority IS NULL OR r.priority = :priority) " +
           "AND (:search IS NULL OR LOWER(r.text) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Recommendation> findForAdmin(@Param("priority") String priority,
                                       @Param("search") String search,
                                       Pageable pageable);
}
