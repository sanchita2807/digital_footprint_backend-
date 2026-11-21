package com.digitalfootprint.digital_footprint_backend.auth;

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
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // ─── Admin queries ───────────────────────────────────────

    long countByIsAdmin(Integer isAdmin);

    long countByIsVerified(boolean isVerified);

    long countByIsActive(boolean isActive);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.isAdmin = 0 AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(u.id AS string) = :search) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isAdmin = 0 AND u.isActive = :isActive AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(u.id AS string) = :search) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchUsersByStatus(@Param("search") String search, @Param("isActive") boolean isActive, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isAdmin = 0 ORDER BY u.createdAt DESC")
    Page<User> findAllRegularUsers(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isAdmin = 0 AND u.isActive = :isActive ORDER BY u.createdAt DESC")
    Page<User> findAllRegularUsersByStatus(@Param("isActive") boolean isActive, Pageable pageable);

    @Query(value = "SELECT DATE(created_at) as dt, COUNT(*) as cnt " +
           "FROM users WHERE created_at >= :startDate AND is_admin = 0 " +
           "GROUP BY DATE(created_at) ORDER BY dt", nativeQuery = true)
    List<Object[]> countDailyRegistrations(@Param("startDate") java.time.LocalDateTime startDate);
}
