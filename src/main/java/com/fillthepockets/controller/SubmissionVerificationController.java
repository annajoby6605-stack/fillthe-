package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.dto.VerifySubmissionRequest;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SubmissionVerificationController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping({"/verify-submission", "/employee/verify-submission"})
    @PreAuthorize("hasRole('PANCHAYAT_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RoadReportResponse>> verifySubmission(
            @RequestBody VerifySubmissionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long targetId = request != null ? request.getEffectiveId() : null;
        if (targetId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission or report ID is required."));
        }

        User employee = (userDetails != null)
                ? userRepository.findById(userDetails.getId()).orElse(null)
                : null;

        String action = (request.getAction() != null) ? request.getAction().trim().toUpperCase() : "APPROVE";

        try {
            if ("REJECT".equals(action)) {
                String reason = (request.getReason() != null && !request.getReason().isBlank())
                        ? request.getReason().trim()
                        : "Rejected during employee inspection";
                RoadReportResponse rejected = reportService.rejectReportAsEmployee(targetId, reason, employee);
                return ResponseEntity.ok(ApiResponse.ok(
                        "Photo rejected. No points were awarded.", rejected));
            } else {
                RoadReportResponse approved = reportService.verifyReportAsEmployee(targetId, employee);
                String villagerName = approved.getVillagerName() != null ? approved.getVillagerName() : "Villager";
                return ResponseEntity.ok(ApiResponse.ok(
                        String.format("Photo verified! 20 points credited to %s.", villagerName), approved));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        }
    }
}
