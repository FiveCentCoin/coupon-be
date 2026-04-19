package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Logistics;

public interface ILogisticsService {

    void save(Logistics logistics);

    void update(Logistics logistics);

    Logistics selectById(Integer id);

    Logistics selectByOrderId(Integer orderId);

    IPage<Logistics> selectPage(Integer pageNum, Integer pageSize, String orderNo);
}
