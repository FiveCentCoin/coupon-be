package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_issue_record")
public class CouponIssueRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer strategyId;
    private String strategyName;
    private Integer userId;
    private Integer userCouponId;
    private Integer couponId;
    private BigDecimal originalAmount;
    private BigDecimal actualAmount;
    private String adjustReason;
    private String userConsumeLevel;
    private String userLoyaltyLevel;
    private String userActivityLevel;
    private String issueSource;
    private String triggerEvent;
    private Integer status;
    private String failReason;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String username;
}
