package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics")
public class Logistics {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private String status;

    private String logisticsCompany;

    private String trackingNo;

    private LocalDateTime shipTime;

    private LocalDateTime receiveTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Orders order;
}
