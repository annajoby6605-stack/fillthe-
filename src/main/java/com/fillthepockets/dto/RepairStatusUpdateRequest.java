package com.fillthepockets.dto;

import com.fillthepockets.entity.RepairStatus;
import jakarta.validation.constraints.NotNull;

public class RepairStatusUpdateRequest {
    @NotNull(message = "New repair status is required")
    private RepairStatus newStatus;

    private String notes;

    public RepairStatusUpdateRequest() {}

    public RepairStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(RepairStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
