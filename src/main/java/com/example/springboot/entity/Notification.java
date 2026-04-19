package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer orderId;

    private String title;

    private String content;

    private String type;

    private Boolean isRead;

    private LocalDateTime readTime;

    private LocalDateTime createTime;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Orders order;
}
