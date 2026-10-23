package com.fillthepockets.dto;

public class PublicStatsResponse {
    private long totalReports;
    private long acceptedReports;
    private long resolvedRoads;
    private long pendingRepairs;
    private long totalPointsAwarded;
    private long registeredVillagers;

    public PublicStatsResponse() {}

    public PublicStatsResponse(long totalReports, long acceptedReports, long resolvedRoads, long pendingRepairs, long totalPointsAwarded, long registeredVillagers) {
        this.totalReports = totalReports;
        this.acceptedReports = acceptedReports;
        this.resolvedRoads = resolvedRoads;
        this.pendingRepairs = pendingRepairs;
        this.totalPointsAwarded = totalPointsAwarded;
        this.registeredVillagers = registeredVillagers;
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

    public long getResolvedRoads() {
        return resolvedRoads;
    }

    public void setResolvedRoads(long resolvedRoads) {
        this.resolvedRoads = resolvedRoads;
    }

    public long getPendingRepairs() {
        return pendingRepairs;
    }

    public void setPendingRepairs(long pendingRepairs) {
        this.pendingRepairs = pendingRepairs;
    }

    public long getTotalPointsAwarded() {
        return totalPointsAwarded;
    }

    public void setTotalPointsAwarded(long totalPointsAwarded) {
        this.totalPointsAwarded = totalPointsAwarded;
    }

    public long getRegisteredVillagers() {
        return registeredVillagers;
    }

    public void setRegisteredVillagers(long registeredVillagers) {
        this.registeredVillagers = registeredVillagers;
    }
}
