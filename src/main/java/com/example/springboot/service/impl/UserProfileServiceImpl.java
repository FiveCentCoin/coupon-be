package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.*;
import com.example.springboot.mapper.*;
import com.example.springboot.service.IUserProfileService;
import com.example.springboot.service.IUserBehaviorLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private CollectMapper collectMapper;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Resource
    private IUserBehaviorLogService userBehaviorLogService;

    @Override
    public UserProfile getOrCreateProfile(Integer userId) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setConsumeLevel("MEDIUM");
            profile.setActivityLevel("ACTIVE");
            profile.setLoyaltyLevel("BRONZE");
            profile.setTotalSpend(BigDecimal.ZERO);
            profile.setOrderCount(0);
            profile.setLoyaltyScore(0);
            profile.setCreateTime(LocalDateTime.now());
            profile.setUpdateTime(LocalDateTime.now());
            save(profile);
        }
        return profile;
    }

    @Override
    public void refreshUserProfile(Integer userId) {
        UserProfile profile = getOrCreateProfile(userId);

        List<Orders> orderList = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId)
        );

        BigDecimal totalSpend = BigDecimal.ZERO;
        int orderCount = 0;
        LocalDateTime lastOrderTime = null;
        Set<String> categories = new HashSet<>();
        List<BigDecimal> orderAmounts = new ArrayList<>();

        for (Orders order : orderList) {
            if ("已付款".equals(order.getState()) || "已完成".equals(order.getState())) {
                BigDecimal price = BigDecimal.valueOf(order.getActualPrice() != null ? order.getActualPrice() : order.getPrice());
                if (price != null) {
                    totalSpend = totalSpend.add(price);
                    orderAmounts.add(price);
                }
                orderCount++;
                if (order.getTime() != null) {
                    LocalDateTime orderTime = parseTimeToLocalDateTime(order.getTime());
                    if (orderTime != null) {
                        if (lastOrderTime == null || orderTime.isAfter(lastOrderTime)) {
                            lastOrderTime = orderTime;
                        }
                    }
                }
            }

            if (order.getGoodsId() != null) {
                Goods goods = goodsMapper.selectById(order.getGoodsId());
                if (goods != null && goods.getTypeId() != null) {
                    categories.add(String.valueOf(goods.getTypeId()));
                }
            }
        }

        profile.setTotalSpend(totalSpend);
        profile.setOrderCount(orderCount);
        profile.setLastOrderTime(lastOrderTime);

        if (!orderAmounts.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal amt : orderAmounts) {
                sum = sum.add(amt);
            }
            profile.setAvgOrderAmount(sum.divide(BigDecimal.valueOf(orderAmounts.size()), 2, RoundingMode.HALF_UP));
        } else {
            profile.setAvgOrderAmount(BigDecimal.ZERO);
        }

        profile.setConsumeLevel(calculateConsumeLevel(totalSpend, orderCount));
        profile.setPreferCategory(categories.isEmpty() ? null : String.join(",", categories));
        profile.setActivityLevel(calculateActivityLevel(userId, lastOrderTime));
        profile.setLoyaltyLevel(calculateLoyaltyLevel(orderCount, totalSpend));
        profile.setLoyaltyScore(calculateLoyaltyScore(orderCount, totalSpend));

        long totalCoupons = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getUserId, userId)
        );
        long usedCoupons = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 1)
        );
        if (totalCoupons > 0) {
            profile.setCouponUseRate(BigDecimal.valueOf(usedCoupons)
                    .divide(BigDecimal.valueOf(totalCoupons), 4, RoundingMode.HALF_UP));
        } else {
            profile.setCouponUseRate(BigDecimal.ZERO);
        }

        profile.setAiTags(generateAiTags(profile));
        profile.setAiRecommendCouponType(recommendCouponType(profile));
        profile.setAiProfileSummary(generateProfileSummary(profile));

        profile.setUpdateTime(LocalDateTime.now());
        updateById(profile);

        logRefreshBehavior(userId, profile);
    }

    private String calculateConsumeLevel(BigDecimal totalSpend, int orderCount) {
        if (totalSpend.compareTo(new BigDecimal("5000")) >= 0 && orderCount >= 10) {
            return "HIGH";
        } else if (totalSpend.compareTo(new BigDecimal("1000")) >= 0 && orderCount >= 3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String calculateActivityLevel(Integer userId, LocalDateTime lastOrderTime) {
        if (lastOrderTime == null) return "NEW";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Long recentOrders = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .in(Orders::getState, Arrays.asList("已付款", "已完成"))
                        .apply("STR_TO_DATE(time, '%Y-%m-%d %H:%i:%s') >= {0}", thirtyDaysAgo)
        );

        if (lastOrderTime.isAfter(sevenDaysAgo)) {
            return "ACTIVE";
        } else if (lastOrderTime.isAfter(thirtyDaysAgo)) {
            return "NORMAL";
        } else if (lastOrderTime.isAfter(now.minusDays(90))) {
            return "SILENT";
        } else {
            return "LOST";
        }
    }

    private String calculateLoyaltyLevel(int orderCount, BigDecimal totalSpend) {
        int score = calculateLoyaltyScore(orderCount, totalSpend);
        if (score >= 800) return "DIAMOND";
        if (score >= 500) return "PLATINUM";
        if (score >= 200) return "SILVER";
        return "BRONZE";
    }

    private int calculateLoyaltyScore(int orderCount, BigDecimal totalSpend) {
        int score = 0;
        score += Math.min(orderCount * 20, 400);
        score += Math.min(totalSpend.divide(new BigDecimal("10"), 0, RoundingMode.DOWN).intValue(), 400);
        score += Math.min(orderCount * 10, 200);
        return Math.min(score, 1000);
    }

    private LocalDateTime parseTimeToLocalDateTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String generateAiTags(UserProfile profile) {
        List<String> tags = new ArrayList<>();
        tags.add("消费能力:" + getLevelName(profile.getConsumeLevel()));
        tags.add("活跃度:" + getLevelName(profile.getActivityLevel()));
        tags.add("忠诚度:" + profile.getLoyaltyLevel());

        if (profile.getCouponUseRate() != null && profile.getCouponUseRate().compareTo(new BigDecimal("0.5")) > 0) {
            tags.add("券敏感型用户");
        }
        if (profile.getOrderCount() != null && profile.getOrderCount() >= 5) {
            tags.add("高复购用户");
        }
        if ("HIGH".equals(profile.getConsumeLevel())) {
            tags.add("高价值客户");
        }
        return String.join("|", tags);
    }

    private Integer recommendCouponType(UserProfile profile) {
        if ("HIGH".equals(profile.getConsumeLevel()) && "ACTIVE".equals(profile.getActivityLevel())) {
            return 1;
        } else if ("LOW".equals(profile.getConsumeLevel())) {
            return 2;
        } else {
            return 3;
        }
    }

    private String generateProfileSummary(UserProfile p) {
        StringBuilder sb = new StringBuilder();
        sb.append(getLevelName(p.getConsumeLevel())).append("消费");
        sb.append(" · ").append(getLevelName(p.getActivityLevel())).append("状态");
        sb.append(" · ").append(p.getLoyaltyLevel()).append("会员");
        if (p.getTotalSpend() != null && p.getTotalSpend().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" · 累计消费¥").append(p.getTotalSpend().setScale(2, RoundingMode.HALF_UP));
        }
        return sb.toString();
    }

    private String getLevelName(String level) {
        if (level == null) return "未知";
        switch (level) {
            case "HIGH": return "高";
            case "MEDIUM": return "中";
            case "LOW": return "低";
            case "ACTIVE": return "活跃";
            case "NORMAL": return "正常";
            case "SILENT": return "低活跃";
            case "LOST": return "流失";
            case "NEW": return "新用户";
            default: return level;
        }
    }

    private void logRefreshBehavior(Integer userId, UserProfile profile) {
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("consumeLevel", profile.getConsumeLevel());
        extraData.put("loyaltyLevel", profile.getLoyaltyLevel());
        extraData.put("activityLevel", profile.getActivityLevel());
        userBehaviorLogService.logBehavior(userId, "PROFILE_REFRESH", "USER", userId,
                "画像更新-" + profile.getLoyaltyLevel(), null);
    }

    @Override
    public void recordBehavior(Integer userId, String behaviorType, String targetType,
                               Integer targetId, String targetName, BigDecimal amount) {
        userBehaviorLogService.logBehavior(userId, behaviorType, targetType, targetId, targetName, amount);

        if ("ORDER".equals(behaviorType) || "PAY".equals(behaviorType) ||
                "COUPON_USE".equals(behaviorType) || "LOGIN".equals(behaviorType)) {
            refreshUserProfile(userId);
        }
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfile> listWithUserDetails(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfile> page) {
        return userProfileMapper.selectPage(page,
                new LambdaQueryWrapper<UserProfile>().orderByDesc(UserProfile::getUpdateTime));
    }

    @Override
    public void refreshAllProfiles() {
        List<UserProfile> allProfiles = this.list();
        for (UserProfile profile : allProfiles) {
            try {
                refreshUserProfile(profile.getUserId());
            } catch (Exception e) {
                log.error("刷新用户画像失败: userId=" + profile.getUserId(), e);
            }
        }
    }
}
