package com.fillthepockets.controller;

import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/reports/pending")
    public ResponseEntity<List<RoadReportResponse>> getPendingReports() {
        return ResponseEntity.ok(reportService.getPendingEmployeeReports());
    }

}
