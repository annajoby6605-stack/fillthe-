package com.fillthepockets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeReportRejectionRequest {

    @NotBlank(message = "A rejection reason is required.")
    @Size(max = 2000, message = "The rejection reason must be 2000 characters or fewer.")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
