package com.fillthepockets.repository;

import com.fillthepockets.entity.PointsTransaction;
import com.fillthepockets.entity.RoadReport;
import com.fillthepockets.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    List<PointsTransaction> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByReport(RoadReport report);
    java.util.Optional<PointsTransaction> findByReport(RoadReport report);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsTransaction p WHERE p.points > 0")
    long sumTotalPointsAwarded();

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsTransaction p WHERE p.user = :user AND p.points > 0")
    long sumTotalPointsAwardedToUser(@Param("user") User user);
}
