package com.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Notification;
import com.example.springboot.service.INotificationService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private INotificationService notificationService;

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        Integer userId = TokenUtils.getCurrentUser().getId();
        IPage<Notification> page = notificationService.selectPage(pageNum, pageSize, userId);
        return Result.success(page);
    }

    @GetMapping("/unread")
    public Result unread() {
        Integer userId = TokenUtils.getCurrentUser().getId();
        Long count = notificationService.countUnread(userId);
        return Result.success(count);
    }

    @GetMapping("/unreadList")
    public Result unreadList() {
        Integer userId = TokenUtils.getCurrentUser().getId();
        return Result.success(notificationService.selectUnreadByUserId(userId));
    }

    @PutMapping("/markAsRead/{id}")
    public Result markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    @PutMapping("/markAllAsRead")
    public Result markAllAsRead() {
        Integer userId = TokenUtils.getCurrentUser().getId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}
