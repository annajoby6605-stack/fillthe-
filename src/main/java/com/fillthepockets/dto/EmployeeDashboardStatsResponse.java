package com.fillthepockets.dto;

public class EmployeeDashboardStatsResponse {
    private long totalReports;
    private long acceptedReports;
    private long rejectedReports;
    private long pendingRepairs;
    private long inProgressRepairs;
    private long resolvedRoads;
    private long totalVillagersReporting;
    private long totalPointsAwarded;
    private long eligibleReports;

    public EmployeeDashboardStatsResponse() {}

    public EmployeeDashboardStatsResponse(long totalReports, long acceptedReports, long rejectedReports,
                                         long pendingRepairs, long inProgressRepairs, long resolvedRoads,
                                         long totalVillagersReporting, long totalPointsAwarded, long eligibleReports) {
        this.totalReports = totalReports;
        this.acceptedReports = acceptedReports;
        this.rejectedReports = rejectedReports;
        this.pendingRepairs = pendingRepairs;
        this.inProgressRepairs = inProgressRepairs;
        this.resolvedRoads = resolvedRoads;
        this.totalVillagersReporting = totalVillagersReporting;
        this.totalPointsAwarded = totalPointsAwarded;
        this.eligibleReports = eligibleReports;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getAcceptedReports() {
        return acceptedReports;
    }

    public void setAcceptedReports(long acceptedReports) {
        this.acceptedReports = acceptedReports;
    }

    public long getRejectedReports() {
        return rejectedReports;
    }

    public void setRejectedReports(long rejectedReports) {
        this.rejectedReports = rejectedReports;
    }

    public long getPendingRepairs() {
        return pendingRepairs;
    }

    public void setPendingRepairs(long pendingRepairs) {
        this.pendingRepairs = pendingRepairs;
    }

    public long getInProgressRepairs() {
        return inProgressRepairs;
    }

    public void setInProgressRepairs(long inProgressRepairs) {
        this.inProgressRepairs = inProgressRepairs;
    }

    public long getResolvedRoads() {
        return resolvedRoads;
    }

    public void setResolvedRoads(long resolvedRoads) {
        this.resolvedRoads = resolvedRoads;
    }

    public long getTotalVillagersReporting() {
        return totalVillagersReporting;
    }

    public void setTotalVillagersReporting(long totalVillagersReporting) {
        this.totalVillagersReporting = totalVillagersReporting;
    }

    public long getTotalPointsAwarded() {
        return totalPointsAwarded;
    }

    public void setTotalPointsAwarded(long totalPointsAwarded) {
        this.totalPointsAwarded = totalPointsAwarded;
    }

    public long getEligibleReports() {
        return eligibleReports;
    }

    public void setEligibleReports(long eligibleReports) {
        this.eligibleReports = eligibleReports;
    }
}
