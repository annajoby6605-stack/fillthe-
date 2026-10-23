package com.fillthepockets.service;

import com.fillthepockets.entity.RewardCoupon;
import com.fillthepockets.entity.RewardRedemption;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.RewardCouponRepository;
import com.fillthepockets.repository.RewardRedemptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardService {

    @Autowired
    private RewardCouponRepository couponRepository;

    @Autowired
    private RewardRedemptionRepository redemptionRepository;

    @Autowired
    private PointsService pointsService;

    private final SecureRandom secureRandom = new SecureRandom();

    public List<RewardCoupon> getActiveCoupons() {
        return couponRepository.findByActiveTrueOrderByPointsRequiredAsc();
    }

    @Transactional
    public RewardRedemption redeemCoupon(Long couponId, User villager) {
        RewardCoupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with ID: " + couponId));

        if (!coupon.getActive()) {
            throw new IllegalArgumentException("This reward is currently inactive.");
        }

        // Generate unique redemption coupon voucher code: FTP-[PREFIX]-[5DIGITS]
        String code = "FTP-" + coupon.getCouponCodePrefix() + "-" + String.format("%05d", secureRandom.nextInt(100000));

        // Deduct points from wallet
        pointsService.deductPointsForReward(villager, coupon, code);

        // Create redemption record valid for 30 days
        RewardRedemption redemption = new RewardRedemption(
                villager,
                coupon,
                code,
                coupon.getPointsRequired(),
                LocalDateTime.now().plusDays(30)
        );

        return redemptionRepository.save(redemption);
    }

    public List<RewardRedemption> getUserRedemptions(User villager) {
        return redemptionRepository.findByUserOrderByRedeemedAtDesc(villager);
    }
}
