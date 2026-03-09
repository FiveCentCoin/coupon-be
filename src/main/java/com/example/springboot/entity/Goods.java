package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("goods")
public class Goods {

    @TableId(type= IdType.AUTO)
    private Integer id;
    private String name;
    private String descr;
    private String content;
    private String cover;
    private Double price;
    private Integer store;
    private Integer userId;
    private String date;
    private Integer typeId;
    private String state;
    private Integer sales;

    @TableField(exist = false)
    private String typeName;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private Boolean isCollect;
}