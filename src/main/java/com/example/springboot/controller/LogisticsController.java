package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Logistics;
import com.example.springboot.entity.Notification;
import com.example.springboot.entity.Orders;
import com.example.springboot.mapper.OrdersMapper;
import com.example.springboot.service.ILogisticsService;
import com.example.springboot.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin
@RestController
@RequestMapping("/logistics")
public class LogisticsController {

    @Autowired
    private ILogisticsService logisticsService;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private OrdersMapper ordersMapper;

    @PostMapping("/ship")
    public Result ship(@RequestBody Logistics logistics) {
        logistics.setShipTime(LocalDateTime.now());
        logisticsService.save(logistics);

        Orders order = ordersMapper.selectById(logistics.getOrderId());

        Notification notification = new Notification();
        notification.setUserId(order.getUserId());
        notification.setOrderId(order.getId());
        notification.setTitle("发货通知");
        notification.setContent("您的订单 " + order.getOrderNo() + " 已发货，物流公司：" + logistics.getLogisticsCompany() + "，物流单号：" + logistics.getTrackingNo());
        notification.setType("发货通知");
        notificationService.save(notification);

        return Result.success();
    }

    @PutMapping("/updateStatus")
    public Result updateStatus(@RequestBody Logistics logistics) {
        logisticsService.update(logistics);

        if ("已签收".equals(logistics.getStatus())) {
            logistics.setReceiveTime(LocalDateTime.now());
            logisticsService.update(logistics);

            Orders order = ordersMapper.selectById(logistics.getOrderId());
            if (order != null) {
                order.setState("已完成");
                ordersMapper.updateById(order);

                Notification notification = new Notification();
                notification.setUserId(order.getUserId());
                notification.setOrderId(order.getId());
                notification.setTitle("签收通知");
                notification.setContent("您的订单 " + order.getOrderNo() + " 已成功签收，感谢您的购买！");
                notification.setType("物流更新");
                notificationService.save(notification);
            }
        }

        return Result.success();
    }

    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id) {
        return Result.success(logisticsService.selectById(id));
    }

    @GetMapping("/selectByOrderId")
    public Result selectByOrderId(@RequestParam Integer orderId) {
        return Result.success(logisticsService.selectByOrderId(orderId));
    }

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String orderNo,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        return Result.success(logisticsService.selectPage(pageNum, pageSize, orderNo));
    }
}
