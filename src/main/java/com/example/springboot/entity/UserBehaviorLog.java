package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_behavior_log")
public class UserBehaviorLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String behaviorType;
    private String targetType;
    private Integer targetId;
    private String targetName;
    private BigDecimal amount;
    private String extraData;
    private String ip;
    private String userAgent;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String behaviorTypeName;
}
