package com.fillthepockets.repository;

import com.fillthepockets.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByEmailAndUsedFalseOrderByIdDesc(String email);
    Optional<OtpRecord> findTopByEmailOrderByIdDesc(String email);
    Optional<OtpRecord> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    Optional<OtpRecord> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
