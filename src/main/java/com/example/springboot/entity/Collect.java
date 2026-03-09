package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("collect")
public class Collect {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer goodsId;
    private String time;

    @TableField(exist = false)
    private Goods goods;

    @TableField(exist = false)
    private String goodsName;

    @TableField(exist = false)
    private String userName;
}