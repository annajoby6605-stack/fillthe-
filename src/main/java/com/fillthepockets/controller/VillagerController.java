package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.VillagerDashboardResponse;
import com.fillthepockets.entity.PointsTransaction;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.PointsService;
import com.fillthepockets.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/villager")
public class VillagerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatsService statsService;

    @Autowired
    private PointsService pointsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<VillagerDashboardResponse>> getDashboard(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User villager = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        VillagerDashboardResponse resp = statsService.getVillagerDashboard(villager);
        return ResponseEntity.ok(ApiResponse.ok("Villager dashboard data retrieved", resp));
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWallet(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User villager = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<PointsTransaction> history = pointsService.getUserTransactionHistory(villager);

        Map<String, Object> data = new HashMap<>();
        data.put("pointsBalance", villager.getPointsBalance());
        data.put("fullName", villager.getFullName());
        data.put("email", villager.getEmail());
        data.put("rewardThreshold", 40);
        data.put("eligibleForRewards", villager.getPointsBalance() >= 40);
        data.put("transactions", history);

        return ResponseEntity.ok(ApiResponse.ok("Wallet details retrieved", data));
    }
}
