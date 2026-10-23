package com.fillthepockets.dto;

public class VerifySubmissionRequest {

    private Long submissionId;
    private Long reportId;
    private String action; // "APPROVE" or "REJECT" (default: "APPROVE")
    private String reason; // optional for rejection

    public VerifySubmissionRequest() {
    }

    public VerifySubmissionRequest(Long submissionId, String action, String reason) {
        this.submissionId = submissionId;
        this.reportId = submissionId;
        this.action = action;
        this.reason = reason;
    }

    public Long getEffectiveId() {
        return submissionId != null ? submissionId : reportId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
