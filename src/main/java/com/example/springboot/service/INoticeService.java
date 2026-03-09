package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Notice;

import java.util.List;

public interface INoticeService {

    /**
     * 新增
     */
    void save(Notice notice);

    /**
     * 修改
     */
    void update(Notice notice);

    /**
     * 删除
     */
    void remove(Integer id);

    /**
     * 查询全部数据
     */
    List<Notice> selectAll();

    /**
     * 根据ID查询
     */
    Notice selectById(Integer id);

    /**
     * 分页查询
     */
    IPage<Notice> selectPage(Integer pageNum, Integer pageSize, String name);
}
