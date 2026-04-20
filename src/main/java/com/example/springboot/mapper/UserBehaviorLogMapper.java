package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {
}
