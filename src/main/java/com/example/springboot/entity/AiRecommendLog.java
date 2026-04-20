package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_recommend_log")
public class AiRecommendLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String scene;
    private String inputData;
    private String availableCoupons;
    private Integer recommendedCouponId;
    private String recommendReason;
    private BigDecimal recommendScore;
    private Integer isAdopted;
    private BigDecimal actualSaveAmount;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String couponName;
}
