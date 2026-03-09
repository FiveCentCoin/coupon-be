package com.example.springboot.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Notice;
import com.example.springboot.mapper.NoticeMapper;
import com.example.springboot.service.INoticeService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements INoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public void save(Notice notice) {
        notice.setTime(DateUtil.now());
        notice.setUserId(TokenUtils.getCurrentUser().getId());
        noticeMapper.insert(notice);
    }

    @Override
    public void update(Notice notice) {
        noticeMapper.updateById(notice);
    }

    @Override
    public void remove(Integer id) {
        noticeMapper.deleteById(id);
    }

    @Override
    public List<Notice> selectAll() {
        return noticeMapper.selectList(null);
    }

    @Override
    public Notice selectById(Integer id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public IPage<Notice> selectPage(Integer pageNum, Integer pageSize, String name) {
        Page<Notice> page = new Page<>(pageNum, pageSize);

        // select * from notice where name like %name%
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Notice::getName,name);

        return noticeMapper.selectPage(page, queryWrapper);
    }
}
