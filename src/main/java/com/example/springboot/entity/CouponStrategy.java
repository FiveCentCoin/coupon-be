package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_strategy")
public class CouponStrategy {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String targetConsumeLevel;
    private String targetActivityLevel;
    private String targetLoyaltyLevel;
    private String targetUserIds;
    private Integer couponTemplateId;
    private Integer issueCountPerUser;
    private Integer totalIssueLimit;
    private Integer issuedCount;
    private Integer enableDynamicAmount;
    private BigDecimal baseDiscountAmount;
    private BigDecimal maxDiscountAmount;
    private BigDecimal amountFactorHigh;
    private BigDecimal amountFactorMedium;
    private BigDecimal amountFactorLow;
    private String triggerType;
    private String triggerEvent;
    private String triggerCron;
    private Integer status;
    private Integer priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String couponTemplateName;

    @TableField(exist = false)
    private Integer matchUserCount;
}
