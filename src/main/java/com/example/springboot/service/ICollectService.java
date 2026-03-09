package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Carousel;
import com.example.springboot.entity.Collect;

import java.util.List;

public interface ICollectService {

    /**
      * 新增
      */
    void save(Collect collect);

    /**
      * 修改
      */
    void update(Collect collect);

    /**
      * 删除
      */
    void remove(Integer id);

    /**
      * 查询全部数据
      */
    List<Collect> selectAll();

    /**
      * 根据ID查询数据
      */
    Collect selectById(Integer id);

    /**
      * 分页查询数据
      */
    IPage<Collect> selectPage(Integer pageNum, Integer pageSize, String name);


    /**
     * 我的收藏
     */
    List<Collect> myCollect();
}