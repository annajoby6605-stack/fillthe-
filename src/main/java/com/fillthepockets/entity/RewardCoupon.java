package com.fillthepockets.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reward_coupons")
public class RewardCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 60)
    private String category; // e.g. "Store Discount", "Solar / Green Perk", "Utility Voucher"

    @Column(nullable = false)
    private Integer pointsRequired;

    @Column(nullable = false, length = 20)
    private String couponCodePrefix;

    @Column(length = 60)
    private String icon;

    @Column(nullable = false)
    private Boolean active = true;

    public RewardCoupon() {}

    public RewardCoupon(String title, String description, String category, Integer pointsRequired, String couponCodePrefix, String icon) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.pointsRequired = pointsRequired;
        this.couponCodePrefix = couponCodePrefix;
        this.icon = icon;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    public String getCouponCodePrefix() {
        return couponCodePrefix;
    }

    public void setCouponCodePrefix(String couponCodePrefix) {
        this.couponCodePrefix = couponCodePrefix;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
