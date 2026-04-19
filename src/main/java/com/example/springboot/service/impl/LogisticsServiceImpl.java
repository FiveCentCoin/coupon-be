package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Logistics;
import com.example.springboot.entity.Orders;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.LogisticsMapper;
import com.example.springboot.mapper.OrdersMapper;
import com.example.springboot.service.ILogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogisticsServiceImpl implements ILogisticsService {

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Override
    public void save(Logistics logistics) {
        Orders order = ordersMapper.selectById(logistics.getOrderId());
        if (order == null) {
            throw new ServiceException("201", "订单不存在");
        }
        if (!"已支付".equals(order.getState())) {
            throw new ServiceException("201", "订单未支付，无法发货");
        }

        LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Logistics::getOrderId, logistics.getOrderId());
        Logistics existLogistics = logisticsMapper.selectOne(queryWrapper);
        if (existLogistics != null) {
            throw new ServiceException("201", "该订单已有物流记录");
        }

        logistics.setStatus("已发货");
        logisticsMapper.insert(logistics);

        order.setState("待发货");
        ordersMapper.updateById(order);
    }

    @Override
    public void update(Logistics logistics) {
        Logistics existLogistics = logisticsMapper.selectById(logistics.getId());
        if (existLogistics == null) {
            throw new ServiceException("201", "物流记录不存在");
        }
        logisticsMapper.updateById(logistics);
    }

    @Override
    public Logistics selectById(Integer id) {
        return logisticsMapper.selectById(id);
    }

    @Override
    public Logistics selectByOrderId(Integer orderId) {
        LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Logistics::getOrderId, orderId);
        return logisticsMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<Logistics> selectPage(Integer pageNum, Integer pageSize, String orderNo) {
        Page<Logistics> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Logistics> queryWrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            LambdaQueryWrapper<Orders> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.like(Orders::getOrderNo, orderNo);
            Orders order = ordersMapper.selectOne(orderQuery);
            if (order != null) {
                queryWrapper.eq(Logistics::getOrderId, order.getId());
            } else {
                queryWrapper.eq(Logistics::getOrderId, -1);
            }
        }

        Page<Logistics> logisticsPage = logisticsMapper.selectPage(page, queryWrapper);
        logisticsPage.getRecords().forEach(logistics -> {
            logistics.setOrder(ordersMapper.selectById(logistics.getOrderId()));
        });
        return logisticsPage;
    }
}
