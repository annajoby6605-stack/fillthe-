package com.fillthepockets.service;

import com.fillthepockets.dto.EmployeeDashboardStatsResponse;
import com.fillthepockets.dto.PublicStatsResponse;
import com.fillthepockets.dto.VillagerDashboardResponse;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.PointsTransactionRepository;
import com.fillthepockets.repository.RoadReportRepository;
import com.fillthepockets.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    @Autowired
    private RoadReportRepository roadReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;

    @Autowired
    private ReportService reportService;

    @Transactional(readOnly = true)
    public PublicStatsResponse getPublicStats() {
        long total = roadReportRepository.count();
        long accepted = roadReportRepository.countByStatusIn(
                java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED));
        long resolved = roadReportRepository.countByRepairStatus(RepairStatus.RESOLVED);
        long pending = roadReportRepository.countByRepairStatus(RepairStatus.PENDING);
        long points = pointsTransactionRepository.sumTotalPointsAwarded();
        long villagers = userRepository.countByRole(Role.VILLAGER);

        return new PublicStatsResponse(total, accepted, resolved, pending, points, villagers);
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardStatsResponse getEmployeeDashboardStats() {
        long total = roadReportRepository.count();
        long accepted = roadReportRepository.countByStatusIn(
                java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED));
        long rejectedUnrelated = roadReportRepository.countByStatus(ReportStatus.REJECTED_UNRELATED);
        long rejectedDuplicate = roadReportRepository.countByStatus(ReportStatus.REJECTED_DUPLICATE);
        long rejectedByAdmin = roadReportRepository.countByStatus(ReportStatus.REJECTED_BY_ADMIN);
        long rejectedByAi = roadReportRepository.countByStatus(ReportStatus.AI_REJECTED);
        long rejectedByEmployee = roadReportRepository.countByStatus(ReportStatus.EMPLOYEE_REJECTED);
        long rejected = rejectedUnrelated + rejectedDuplicate + rejectedByAdmin
                + rejectedByAi + rejectedByEmployee;

        long pending = roadReportRepository.countByStatusInAndRepairStatus(
                java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED), RepairStatus.PENDING);
        long inProgress = roadReportRepository.countByStatusInAndRepairStatus(
                java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED), RepairStatus.IN_PROGRESS);
        long resolved = roadReportRepository.countByStatusInAndRepairStatus(
                java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED), RepairStatus.RESOLVED);

        long villagersReporting = roadReportRepository.countDistinctVillagersWithReports();
        long pointsAwarded = pointsTransactionRepository.sumTotalPointsAwarded();
        long eligibleReports = accepted;

        return new EmployeeDashboardStatsResponse(total, accepted, rejected, pending, inProgress, resolved,
                villagersReporting, pointsAwarded, eligibleReports);
    }

    @Transactional(readOnly = true)
    public VillagerDashboardResponse getVillagerDashboard(User villager) {
        // Re-read fresh user balance
        User fresh = userRepository.findById(villager.getId()).orElse(villager);

        long submitted = roadReportRepository.countByVillager(fresh);
        long accepted = roadReportRepository.countByVillagerAndStatusIn(
                fresh, java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED));
        long rejectedUnrelated = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.REJECTED_UNRELATED);
        long rejectedDuplicate = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.REJECTED_DUPLICATE);
        long rejectedByAdmin = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.REJECTED_BY_ADMIN);
        long rejectedByAi = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.AI_REJECTED);
        long rejectedByEmployee = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.EMPLOYEE_REJECTED);
        long pending = roadReportRepository.countByVillagerAndStatusIn(fresh,
                java.util.List.of(ReportStatus.PENDING_EMPLOYEE_VERIFICATION,
                        ReportStatus.PENDING_ADMIN_APPROVAL, ReportStatus.NEEDS_REVIEW));
        long inProgress = roadReportRepository.countByVillagerAndStatusInAndRepairStatus(
                fresh, java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED), RepairStatus.IN_PROGRESS);
        long resolved = roadReportRepository.countByVillagerAndStatusInAndRepairStatus(
                fresh, java.util.List.of(ReportStatus.ACCEPTED, ReportStatus.EMPLOYEE_APPROVED), RepairStatus.RESOLVED);
        long totalPointsEarned = pointsTransactionRepository.sumTotalPointsAwardedToUser(fresh);

        VillagerDashboardResponse resp = new VillagerDashboardResponse();
        resp.setFullName(fresh.getFullName());
        resp.setEmail(fresh.getEmail());
        resp.setPointsBalance(fresh.getPointsBalance());
        resp.setTotalSubmitted(submitted);
        resp.setTotalAccepted(accepted);
        long totalRejected = rejectedUnrelated + rejectedDuplicate + rejectedByAdmin
                + rejectedByAi + rejectedByEmployee;
        resp.setTotalRejected(totalRejected);
        resp.setPendingReports(pending);
        resp.setInProgressReports(inProgress);
        resp.setResolvedReports(resolved);
        resp.setTotalPointsEarned(totalPointsEarned);
        resp.setEligibleForRewards(fresh.getPointsBalance() >= 40); // 40 pts threshold for first coupon
        resp.setRecentReports(reportService.getVillagerReports(fresh));

        return resp;
    }
}
