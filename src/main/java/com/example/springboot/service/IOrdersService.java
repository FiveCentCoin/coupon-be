package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Orders;

import java.util.List;

public interface IOrdersService {

    /**
     * 新增
     */
    void save(Orders orders);

    /**
     * 修改
     */
    void update(Orders orders);

    /**
     * 删除
     */
    void remove(Integer id);

    /**
     * 查询全部数据
     */
    List<Orders> selectAll();

    /**
     * 根据ID查询
     */
    Orders selectById(Integer id);

    /**
     * 分页查询
     */
    IPage<Orders> selectPage(Integer pageNum, Integer pageSize, String name, String orderNo);

    /**
     * 支付功能
     */
    void pay(Orders orders);
}
