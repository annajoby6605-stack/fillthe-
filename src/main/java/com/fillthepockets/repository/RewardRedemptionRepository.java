package com.fillthepockets.repository;

import com.fillthepockets.entity.RewardRedemption;
import com.fillthepockets.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    List<RewardRedemption> findByUserOrderByRedeemedAtDesc(User user);
    Optional<RewardRedemption> findByRedemptionCode(String redemptionCode);
}
