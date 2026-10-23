package com.fillthepockets.repository;

import com.fillthepockets.entity.RepairStatusUpdate;
import com.fillthepockets.entity.RoadReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairStatusUpdateRepository extends JpaRepository<RepairStatusUpdate, Long> {
    List<RepairStatusUpdate> findByReportOrderByIdDesc(RoadReport report);
    List<RepairStatusUpdate> findTop10ByOrderByIdDesc();
}
