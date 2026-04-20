package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.AiRecommendLog;

import java.util.List;
import java.util.Map;

public interface IAiRecommendLogService extends IService<AiRecommendLog> {

    void logRecommendation(Integer userId, String scene, String inputData,
                           String availableCoupons, Integer recommendedCouponId,
                           String recommendReason, java.math.BigDecimal recommendScore);

    List<AiRecommendLog> getUserRecommendHistory(Integer userId, int limit);
}
