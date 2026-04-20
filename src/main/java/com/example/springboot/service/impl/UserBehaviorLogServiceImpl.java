package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.UserBehaviorLog;
import com.example.springboot.mapper.UserBehaviorLogMapper;
import com.example.springboot.service.IUserBehaviorLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserBehaviorLogServiceImpl extends ServiceImpl<UserBehaviorLogMapper, UserBehaviorLog> implements IUserBehaviorLogService {

    @Resource
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @Override
    public void logBehavior(Integer userId, String behaviorType, String targetType,
                            Integer targetId, String targetName, BigDecimal amount) {
        UserBehaviorLog log = new UserBehaviorLog();
        log.setUserId(userId);
        log.setBehaviorType(behaviorType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setAmount(amount);
        log.setCreateTime(LocalDateTime.now());
        save(log);
    }
}
