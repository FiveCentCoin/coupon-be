package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Carousel;

import java.util.List;

public interface ICarouselService {

    /**
      * 新增
      */
    void save(Carousel carousel);

    /**
      * 修改
      */
    void update(Carousel carousel);

    /**
      * 删除
      */
    void remove(Integer id);

    /**
      * 查询全部数据
      */
    List<Carousel> selectAll();

    /**
      * 根据ID查询数据
      */
    Carousel selectById(Integer id);

    /**
      * 分页查询数据
      */
    IPage<Carousel> selectPage(Integer pageNum, Integer pageSize, String name);

}