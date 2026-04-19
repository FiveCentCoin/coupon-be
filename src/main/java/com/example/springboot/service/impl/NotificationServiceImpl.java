package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Notification;
import com.example.springboot.mapper.NotificationMapper;
import com.example.springboot.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public void save(Notification notification) {
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    @Override
    public Notification selectById(Integer id) {
        return notificationMapper.selectById(id);
    }

    @Override
    public List<Notification> selectUnreadByUserId(Integer userId) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                   .eq(Notification::getIsRead, false)
                   .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(queryWrapper);
    }

    @Override
    public IPage<Notification> selectPage(Integer pageNum, Integer pageSize, Integer userId) {
        Page<Notification> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                   .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Long countUnread(Integer userId) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notification::getUserId, userId)
                   .eq(Notification::getIsRead, false);
        return notificationMapper.selectCount(queryWrapper);
    }

    @Override
    public void markAsRead(Integer id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && !notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public void markAllAsRead(Integer userId) {
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                    .eq(Notification::getIsRead, false)
                    .set(Notification::getIsRead, true)
                    .set(Notification::getReadTime, LocalDateTime.now());
        notificationMapper.update(null, updateWrapper);
    }
}
