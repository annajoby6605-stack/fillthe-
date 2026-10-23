package com.fillthepockets.service;

import com.fillthepockets.entity.*;
import com.fillthepockets.repository.AiVerificationRepository;
import com.fillthepockets.repository.PointsTransactionRepository;
import com.fillthepockets.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);
    public static final int REPORT_REWARD_POINTS = 20;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;

    @Autowired
    private AiVerificationRepository aiVerificationRepository;

    /**
     * Award 20 points for an eligible, verified road damage report.
     * Guaranteed atomic and duplicate-reward protected.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PointsTransaction awardReportPoints(User villager, RoadReport report) {
        if (report.getStatus() != ReportStatus.PENDING_EMPLOYEE_VERIFICATION) {
            throw new IllegalArgumentException("Points can only be awarded for an AI-approved report awaiting employee verification.");
        }

        AiVerificationResult verification = aiVerificationRepository.findByReport(report)
                .orElseThrow(() -> new IllegalStateException("AI verification result is missing for this report."));
        if (!"ROAD_DAMAGE".equalsIgnoreCase(verification.getCategory())) {
            throw new IllegalArgumentException("Points can only be awarded after the AI accepts the road-damage image.");
        }

        // Double check no points were already awarded for this report
        if (pointsTransactionRepository.existsByReport(report)) {
            log.warn("Report {} already had points awarded. Duplicate reward aborted.", report.getReportNumber());
            return null;
        }

        // Re-read user to ensure freshest balance
        User freshUser = userRepository.findByIdForUpdate(villager.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        int newBalance = freshUser.getPointsBalance() + REPORT_REWARD_POINTS;
        freshUser.setPointsBalance(newBalance);
        userRepository.save(freshUser);

        String description = "Reward: Verified Road Damage Report (" + report.getReportNumber() + ") - " + report.getRoadName();
        PointsTransaction tx = new PointsTransaction(
                freshUser,
                report,
                REPORT_REWARD_POINTS,
                TransactionType.EARNED_REPORT,
                description,
                newBalance
        );
        tx = pointsTransactionRepository.save(tx);

        report.setPointsAwarded(REPORT_REWARD_POINTS);
        report.setPointsTransactionId(tx.getId());

        log.info("Awarded {} points to {} for report {}. New balance: {}",
                REPORT_REWARD_POINTS, freshUser.getEmail(), report.getReportNumber(), newBalance);

        return tx;
    }

    /**
     * Deduct points atomically for coupon redemption.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PointsTransaction deductPointsForReward(User villager, RewardCoupon coupon, String redemptionCode) {
        User freshUser = userRepository.findByIdForUpdate(villager.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (freshUser.getPointsBalance() < coupon.getPointsRequired()) {
            throw new IllegalArgumentException(String.format(
                    "Insufficient points balance (%d pts). %s requires %d pts.",
                    freshUser.getPointsBalance(), coupon.getTitle(), coupon.getPointsRequired()
            ));
        }

        int newBalance = freshUser.getPointsBalance() - coupon.getPointsRequired();
        freshUser.setPointsBalance(newBalance);
        userRepository.save(freshUser);

        String description = "Redemption: " + coupon.getTitle() + " (Code: " + redemptionCode + ")";
        PointsTransaction tx = new PointsTransaction(
                freshUser,
                null,
                -coupon.getPointsRequired(),
                TransactionType.REDEEMED_COUPON,
                description,
                newBalance
        );
        return pointsTransactionRepository.save(tx);
    }

    public List<PointsTransaction> getUserTransactionHistory(User user) {
        return pointsTransactionRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
