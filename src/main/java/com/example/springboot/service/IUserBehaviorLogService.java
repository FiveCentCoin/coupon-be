package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.UserBehaviorLog;

public interface IUserBehaviorLogService extends IService<UserBehaviorLog> {

    void logBehavior(Integer userId, String behaviorType, String targetType,
                    Integer targetId, String targetName, java.math.BigDecimal amount);
}
