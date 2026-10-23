package com.fillthepockets.dto;

import com.fillthepockets.entity.RepairStatus;
import com.fillthepockets.entity.ReportStatus;
import java.time.LocalDateTime;

public class RoadReportResponse {
    private Long id;
    private String reportNumber;
    private Long villagerId;
    private String villagerName;
    private String villagerEmail;
    private String roadName;
    private String villageArea;
    private String description;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private ReportStatus status;
    private RepairStatus repairStatus;
    private Integer pointsAwarded;
    private String rejectionReason;
    private Integer villagerPointsBalance;
    private Long villagerTotalReports;
    private Long villagerTotalPointsEarned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Employee verification details
    private String verifiedByEmployeeName;
    private String verifiedByEmployeeCode;
    private LocalDateTime verifiedAt;
    private boolean pointsGranted;

    // AI details
    private String aiCategory;
    private Double aiConfidenceScore;
    private String aiExplanation;
    private String aiTopLabels;

    public RoadReportResponse() {}

    public Long getVillagerTotalReports() {
        return villagerTotalReports;
    }

    public void setVillagerTotalReports(Long villagerTotalReports) {
        this.villagerTotalReports = villagerTotalReports;
    }

    public Long getVillagerTotalPointsEarned() {
        return villagerTotalPointsEarned;
    }

    public void setVillagerTotalPointsEarned(Long villagerTotalPointsEarned) {
        this.villagerTotalPointsEarned = villagerTotalPointsEarned;
    }

    public Integer getVillagerPointsBalance() {
        return villagerPointsBalance;
    }

    public void setVillagerPointsBalance(Integer villagerPointsBalance) {
        this.villagerPointsBalance = villagerPointsBalance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public Long getVillagerId() {
        return villagerId;
    }

    public void setVillagerId(Long villagerId) {
        this.villagerId = villagerId;
    }

    public String getVillagerName() {
        return villagerName;
    }

    public void setVillagerName(String villagerName) {
        this.villagerName = villagerName;
    }

    public String getVillagerEmail() {
        return villagerEmail;
    }

    public void setVillagerEmail(String villagerEmail) {
        this.villagerEmail = villagerEmail;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getVillageArea() {
        return villageArea;
    }

    public void setVillageArea(String villageArea) {
        this.villageArea = villageArea;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public RepairStatus getRepairStatus() {
        return repairStatus;
    }

    public void setRepairStatus(RepairStatus repairStatus) {
        this.repairStatus = repairStatus;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAiCategory() {
        return aiCategory;
    }

    public void setAiCategory(String aiCategory) {
        this.aiCategory = aiCategory;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public String getAiTopLabels() {
        return aiTopLabels;
    }

    public void setAiTopLabels(String aiTopLabels) {
        this.aiTopLabels = aiTopLabels;
    }

    public String getVerifiedByEmployeeName() {
        return verifiedByEmployeeName;
    }

    public void setVerifiedByEmployeeName(String verifiedByEmployeeName) {
        this.verifiedByEmployeeName = verifiedByEmployeeName;
    }

    public String getVerifiedByEmployeeCode() {
        return verifiedByEmployeeCode;
    }

    public void setVerifiedByEmployeeCode(String verifiedByEmployeeCode) {
        this.verifiedByEmployeeCode = verifiedByEmployeeCode;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public boolean isPointsGranted() {
        return pointsGranted;
    }

    public void setPointsGranted(boolean pointsGranted) {
        this.pointsGranted = pointsGranted;
    }
}
