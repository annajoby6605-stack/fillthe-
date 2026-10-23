package com.fillthepockets.repository;

import com.fillthepockets.entity.RepairStatus;
import com.fillthepockets.entity.ReportStatus;
import com.fillthepockets.entity.RoadReport;
import com.fillthepockets.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadReportRepository extends JpaRepository<RoadReport, Long> {
    List<RoadReport> findByVillagerOrderByCreatedAtDesc(User villager);
    List<RoadReport> findAllByOrderByCreatedAtDesc();
    List<RoadReport> findByStatusInOrderByCreatedAtDesc(List<ReportStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoadReport r WHERE r.id = :id")
    Optional<RoadReport> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(ReportStatus status);
    long countByStatusIn(List<ReportStatus> statuses);
    long countByRepairStatus(RepairStatus repairStatus);
    long countByStatusAndRepairStatus(ReportStatus status, RepairStatus repairStatus);
    long countByStatusInAndRepairStatus(List<ReportStatus> statuses, RepairStatus repairStatus);
    long countByVillager(User villager);
    long countByVillagerAndStatus(User villager, ReportStatus status);
    long countByVillagerAndStatusIn(User villager, List<ReportStatus> statuses);
    long countByVillagerAndRepairStatus(User villager, RepairStatus repairStatus);
    long countByVillagerAndStatusAndRepairStatus(User villager, ReportStatus status, RepairStatus repairStatus);
    long countByVillagerAndStatusInAndRepairStatus(
            User villager, List<ReportStatus> statuses, RepairStatus repairStatus);

    @Query("SELECT COUNT(DISTINCT r.villager) FROM RoadReport r")
    long countDistinctVillagersWithReports();

    Optional<RoadReport> findByReportNumber(String reportNumber);

    // Find candidate reports for duplicate checking within latitude/longitude bounding box
    @Query("SELECT r FROM RoadReport r WHERE r.latitude BETWEEN :minLat AND :maxLat AND r.longitude BETWEEN :minLon AND :maxLon")
    List<RoadReport> findReportsInArea(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    @Query("SELECT r FROM RoadReport r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:repairStatus IS NULL OR r.repairStatus = :repairStatus) AND " +
           "(:keyword IS NULL OR LOWER(r.roadName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.villageArea) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.reportNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.villager.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.villager.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<RoadReport> searchReports(
            @Param("status") ReportStatus status,
            @Param("repairStatus") RepairStatus repairStatus,
            @Param("keyword") String keyword);
}
