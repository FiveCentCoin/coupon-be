package com.example.springboot.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.Orders;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.GoodsMapper;
import com.example.springboot.mapper.OrdersMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IOrdersService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class OrdersServiceImpl implements IOrdersService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void save(Orders orders) {
        // 1、判断商品库存是否充足，如果不充足，给提示
        Goods goods = goodsMapper.selectById(orders.getGoodsId());
        // 2、如果商品库存充足，就下单（数据库新增一条订单）
        if (goods.getStore() < orders.getNums()){
            throw new ServiceException("201", goods.getName() + "商品库存不足");
        }

        // 3、新增订单
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmsss");
        orders.setOrderNo(sdf.format(new Date()));
        orders.setTime(DateUtil.now());
        ordersMapper.insert(orders);

        // 4、商品库存减去对应的数量
        goods.setStore(goods.getStore() - orders.getNums());
        // 5、销量累加对应的数量
        goods.setSales(goods.getSales() + orders.getNums());
        // 6、更新一下商品
        goodsMapper.updateById(goods);
    }

    @Override
    public void update(Orders orders) {
        ordersMapper.updateById(orders);
    }

    @Override
    public void remove(Integer id) {
        ordersMapper.deleteById(id);
    }

    @Override
    public List<Orders> selectAll() {
        return ordersMapper.selectList(null);
    }

    @Override
    public Orders selectById(Integer id) {
        return ordersMapper.selectById(id);
    }

    @Override
    public IPage<Orders> selectPage(Integer pageNum, Integer pageSize, String name, String orderNo) {
        Page<Orders> page = new Page<>(pageNum, pageSize);

        // select * from orders where name like %name% or order_no = xx
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Orders::getName,name);
        queryWrapper.like(Orders::getOrderNo,orderNo);

        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser.getRole().equals("USER")){
            queryWrapper.eq(Orders::getUserId, currentUser.getId());
        }

        Page<Orders> ordersPage = ordersMapper.selectPage(page, queryWrapper);
        ordersPage.getRecords().stream().forEach(orders -> {
            orders.setGoods(goodsMapper.selectById(orders.getGoodsId()));
            orders.setUser(userMapper.selectById(orders.getUserId()));
        });
        return ordersPage;
    }

    @Override
    public void pay(Orders orders) {
        // 1、查询登录用户的余额是否充足
        User user = userMapper.selectById(orders.getUserId());
        // 2、如果充足，改变订单状态并且余额减去对应的值
        if (user.getAccount() >= orders.getPrice()){
            orders.setState("已支付");
            ordersMapper.updateById(orders);

            // 更新余额
            user.setAccount(user.getAccount() - orders.getPrice());
            userMapper.updateById(user);
        } else{
            // 3、如果余额不足，就给前台提示 ’余额不足，请充值‘
            throw new ServiceException("201", "余额不足，请充值~");
        }
    }
}
