package com.fillthepockets.dto;

import jakarta.validation.constraints.NotNull;

public class RedeemCouponRequest {
    @NotNull(message = "Coupon ID is required")
    private Long couponId;

    public RedeemCouponRequest() {}

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }
}
