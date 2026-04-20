package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String consumeLevel;
    private BigDecimal totalSpend;
    private Integer orderCount;
    private BigDecimal avgOrderAmount;
    private String preferCategory;
    private String preferPriceRange;
    private String priceSensitivity;
    private String activityLevel;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastOrderTime;
    @TableField("login_days_30")
    private Integer loginDays30;
    @TableField("order_days_30")
    private Integer orderDays30;
    private String loyaltyLevel;
    private Integer loyaltyScore;
    private Integer continuousDays;
    private BigDecimal couponUseRate;
    private String aiTags;
    private Integer aiRecommendCouponType;
    private String aiProfileSummary;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String avatar;
}
