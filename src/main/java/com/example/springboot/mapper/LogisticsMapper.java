package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.entity.Logistics;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogisticsMapper extends BaseMapper<Logistics> {
}
