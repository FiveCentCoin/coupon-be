package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.UserProfile;

public interface IUserProfileService extends IService<UserProfile> {

    UserProfile getOrCreateProfile(Integer userId);

    void refreshUserProfile(Integer userId);

    void recordBehavior(Integer userId, String behaviorType, String targetType,
                        Integer targetId, String targetName, java.math.BigDecimal amount);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfile> listWithUserDetails(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfile> page);

    void refreshAllProfiles();
}
