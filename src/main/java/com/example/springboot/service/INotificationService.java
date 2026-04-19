package com.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.entity.Notification;

import java.util.List;

public interface INotificationService {

    void save(Notification notification);

    Notification selectById(Integer id);

    List<Notification> selectUnreadByUserId(Integer userId);

    IPage<Notification> selectPage(Integer pageNum, Integer pageSize, Integer userId);

    Long countUnread(Integer userId);

    void markAsRead(Integer id);

    void markAllAsRead(Integer userId);
}
