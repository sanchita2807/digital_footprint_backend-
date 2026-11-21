package com.digitalfootprint.digital_footprint_backend.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(String phoneNumber);

    List<Otp> findAllByPhoneNumberAndIsUsedFalse(String phoneNumber);

    @Modifying
    @Query("UPDATE Otp o SET o.isUsed = true WHERE o.phoneNumber = :phoneNumber AND o.isUsed = false")
    void invalidateAllActiveByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
