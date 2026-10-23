package com.fillthepockets.dto;

import java.util.List;

public class VillagerDashboardResponse {
    private String fullName;
    private String email;
    private Integer pointsBalance;
    private long totalSubmitted;
    private long totalAccepted;
    private long totalRejected;
    private long pendingReports;
    private long inProgressReports;
    private long resolvedReports;
    private long totalPointsEarned;
    private boolean eligibleForRewards;
    private List<RoadReportResponse> recentReports;

    public VillagerDashboardResponse() {}

    public long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(long pendingReports) {
        this.pendingReports = pendingReports;
    }

    public long getInProgressReports() {
        return inProgressReports;
    }

    public void setInProgressReports(long inProgressReports) {
        this.inProgressReports = inProgressReports;
    }

    public long getResolvedReports() {
        return resolvedReports;
    }

    public void setResolvedReports(long resolvedReports) {
        this.resolvedReports = resolvedReports;
    }

    public long getTotalPointsEarned() {
        return totalPointsEarned;
    }

    public void setTotalPointsEarned(long totalPointsEarned) {
        this.totalPointsEarned = totalPointsEarned;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public long getTotalSubmitted() {
        return totalSubmitted;
    }

    public void setTotalSubmitted(long totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    public long getTotalAccepted() {
        return totalAccepted;
    }

    public void setTotalAccepted(long totalAccepted) {
        this.totalAccepted = totalAccepted;
    }

    public long getTotalRejected() {
        return totalRejected;
    }

    public void setTotalRejected(long totalRejected) {
        this.totalRejected = totalRejected;
    }

    public boolean isEligibleForRewards() {
        return eligibleForRewards;
    }

    public void setEligibleForRewards(boolean eligibleForRewards) {
        this.eligibleForRewards = eligibleForRewards;
    }

    public List<RoadReportResponse> getRecentReports() {
        return recentReports;
    }

    public void setRecentReports(List<RoadReportResponse> recentReports) {
        this.recentReports = recentReports;
    }
}
