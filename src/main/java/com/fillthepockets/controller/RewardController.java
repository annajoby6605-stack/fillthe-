package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.RedeemCouponRequest;
import com.fillthepockets.entity.RewardCoupon;
import com.fillthepockets.entity.RewardRedemption;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    @Autowired
    private RewardService rewardService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<List<RewardCoupon>>> getCoupons() {
        List<RewardCoupon> coupons = rewardService.getActiveCoupons();
        return ResponseEntity.ok(ApiResponse.ok("Active reward coupons retrieved", coupons));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<RewardRedemption>> redeemCoupon(
            @Valid @RequestBody RedeemCouponRequest req,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            User villager = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            RewardRedemption redemption = rewardService.redeemCoupon(req.getCouponId(), villager);
            return ResponseEntity.ok(ApiResponse.ok("Coupon redeemed successfully! Present this code at the village center.", redemption));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-redemptions")
    public ResponseEntity<ApiResponse<List<RewardRedemption>>> getMyRedemptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User villager = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<RewardRedemption> redemptions = rewardService.getUserRedemptions(villager);
        return ResponseEntity.ok(ApiResponse.ok("Redemption vouchers retrieved", redemptions));
    }
}
