package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.AiRecommendLog;
import com.example.springboot.mapper.AiRecommendLogMapper;
import com.example.springboot.service.IAiRecommendLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AiRecommendLogServiceImpl extends ServiceImpl<AiRecommendLogMapper, AiRecommendLog> implements IAiRecommendLogService {

    @Autowired
    private AiRecommendLogMapper aiRecommendLogMapper;

    @Override
    public void logRecommendation(Integer userId, String scene, String inputData,
                                   String availableCoupons, Integer recommendedCouponId,
                                   String recommendReason, BigDecimal recommendScore) {
        AiRecommendLog log = new AiRecommendLog();
        log.setUserId(userId);
        log.setScene(scene);
        log.setInputData(inputData);
        log.setAvailableCoupons(availableCoupons);
        log.setRecommendedCouponId(recommendedCouponId);
        log.setRecommendReason(recommendReason);
        log.setRecommendScore(recommendScore);
        save(log);
    }

    @Override
    public List<AiRecommendLog> getUserRecommendHistory(Integer userId, int limit) {
        return aiRecommendLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiRecommendLog>()
                        .eq(AiRecommendLog::getUserId, userId)
                        .orderByDesc(AiRecommendLog::getCreateTime)
                        .last("LIMIT " + limit)
        );
    }
}
