package com.fillthepockets.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_redemptions")
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coupon_id", nullable = false)
    private RewardCoupon coupon;

    @Column(nullable = false, unique = true, length = 40)
    private String redemptionCode;

    @Column(nullable = false)
    private Integer pointsDeducted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RedemptionStatus status = RedemptionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime redeemedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime validUntil;

    public RewardRedemption() {}

    public RewardRedemption(User user, RewardCoupon coupon, String redemptionCode, Integer pointsDeducted, LocalDateTime validUntil) {
        this.user = user;
        this.coupon = coupon;
        this.redemptionCode = redemptionCode;
        this.pointsDeducted = pointsDeducted;
        this.status = RedemptionStatus.ACTIVE;
        this.redeemedAt = LocalDateTime.now();
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RewardCoupon getCoupon() {
        return coupon;
    }

    public void setCoupon(RewardCoupon coupon) {
        this.coupon = coupon;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public void setRedemptionCode(String redemptionCode) {
        this.redemptionCode = redemptionCode;
    }

    public Integer getPointsDeducted() {
        return pointsDeducted;
    }

    public void setPointsDeducted(Integer pointsDeducted) {
        this.pointsDeducted = pointsDeducted;
    }

    public RedemptionStatus getStatus() {
        return status;
    }

    public void setStatus(RedemptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
}
