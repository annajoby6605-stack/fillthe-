package com.fillthepockets.repository;

import com.fillthepockets.entity.RewardCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardCouponRepository extends JpaRepository<RewardCoupon, Long> {
    List<RewardCoupon> findByActiveTrueOrderByPointsRequiredAsc();
}
