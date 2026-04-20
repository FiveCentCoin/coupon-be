package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.CouponStrategy;

import java.util.List;
import java.util.Map;

public interface ICouponStrategyService extends IService<CouponStrategy> {

    List<CouponStrategy> getActiveStrategies();

    List<CouponStrategy> matchStrategiesForUser(Integer userId);

    Map<String, Object> executeStrategy(Integer strategyId);

    Map<String, Object> executeStrategyForUser(Integer strategyId, Integer userId);
}
