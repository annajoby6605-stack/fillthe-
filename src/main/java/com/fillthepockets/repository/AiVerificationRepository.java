package com.fillthepockets.repository;

import com.fillthepockets.entity.AiVerificationResult;
import com.fillthepockets.entity.RoadReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiVerificationRepository extends JpaRepository<AiVerificationResult, Long> {
    Optional<AiVerificationResult> findByReport(RoadReport report);
}
