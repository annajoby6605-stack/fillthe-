package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.PublicStatsResponse;
import com.fillthepockets.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class PublicStatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicStatsResponse>> getPublicStats() {
        PublicStatsResponse stats = statsService.getPublicStats();
        return ResponseEntity.ok(ApiResponse.ok("Live village statistics retrieved", stats));
    }
}
