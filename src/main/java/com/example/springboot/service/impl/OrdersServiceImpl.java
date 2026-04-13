package com.example.springboot.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.Orders;
import com.example.springboot.entity.User;
import com.example.springboot.entity.UserCoupon;
import com.example.springboot.entity.Coupon;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.GoodsMapper;
import com.example.springboot.mapper.OrdersMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IOrdersService;
import com.example.springboot.service.IUserCouponService;
import com.example.springboot.service.ICouponService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Autowired
    private IUserCouponService userCouponService;

    @Autowired
    private ICouponService couponService;

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
        // 1、计算实际支付金额
        double actualPrice = orders.getPrice();
        if (orders.getCouponId() != null) {
            // 2、查询用户优惠券
            UserCoupon userCoupon = userCouponService.getById(orders.getCouponId());
            if (userCoupon == null) {
                throw new ServiceException("201", "优惠券不存在");
            }
            if (userCoupon.getStatus() != 0) {
                throw new ServiceException("201", "优惠券已使用或过期");
            }
            if (!userCoupon.getUserId().equals((long) orders.getUserId())) {
                throw new ServiceException("201", "优惠券不属于当前用户");
            }
            
            // 3、查询优惠券信息
            Coupon coupon = couponService.getById(userCoupon.getCouponId());
            if (coupon == null) {
                throw new ServiceException("201", "优惠券信息不存在");
            }
            if (coupon.getStatus() != 1) {
                throw new ServiceException("201", "优惠券已禁用");
            }
            
            // 4、计算优惠金额
            if (coupon.getType() == 1) { // 满减券
                if (actualPrice >= coupon.getMinAmount().doubleValue()) {
                    actualPrice -= coupon.getDiscountAmount().doubleValue();
                } else {
                    throw new ServiceException("201", "未达到满减条件");
                }
            } else if (coupon.getType() == 2) { // 无门槛券
                actualPrice -= coupon.getDiscountAmount().doubleValue();
                if (actualPrice < 0) {
                    actualPrice = 0;
                }
            } else if (coupon.getType() == 3) { // 折扣券
                actualPrice *= coupon.getDiscountRate().doubleValue() / 100;
            }
            
            // 5、更新优惠券状态
            userCoupon.setStatus(1);
            userCoupon.setUseTime(java.time.LocalDateTime.now());
            userCoupon.setOrderId((long) orders.getId());
            userCouponService.updateById(userCoupon);
        }
        
        // 6、查询登录用户的余额是否充足
        User user = userMapper.selectById(orders.getUserId());
        if (user.getAccount() < actualPrice) {
            throw new ServiceException("201", "余额不足，请充值~");
        }
        
        // 7、更新订单状态和实际支付金额
        orders.setState("已支付");
        orders.setActualPrice(actualPrice);
        ordersMapper.updateById(orders);

        // 8、更新余额
        user.setAccount(user.getAccount() - actualPrice);
        userMapper.updateById(user);
    }

    @Override
    public void batchPay(List<Integer> orderIds, Long couponId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new ServiceException("201", "订单列表不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Integer orderId : orderIds) {
            Orders order = ordersMapper.selectById(orderId);
            if (order == null) {
                throw new ServiceException("201", "订单ID " + orderId + " 不存在");
            }
            if (!"待付款".equals(order.getState())) {
                throw new ServiceException("201", "订单ID " + orderId + " 不是待付款状态");
            }
            totalAmount = totalAmount.add(new BigDecimal(order.getPrice().toString()));
        }

        BigDecimal actualTotalPrice = totalAmount;
        if (couponId != null) {
            UserCoupon userCoupon = userCouponService.getById(couponId);
            if (userCoupon == null) {
                throw new ServiceException("201", "优惠券不存在");
            }
            if (userCoupon.getStatus() != 0) {
                throw new ServiceException("201", "优惠券已使用或过期");
            }

            Coupon coupon = couponService.getById(userCoupon.getCouponId());
            if (coupon == null) {
                throw new ServiceException("201", "优惠券信息不存在");
            }
            if (coupon.getStatus() != 1) {
                throw new ServiceException("201", "优惠券已禁用");
            }

            if (coupon.getType() == 1) {
                if (totalAmount.compareTo(coupon.getMinAmount()) < 0) {
                    throw new ServiceException("201", "未达到满减条件");
                }
                actualTotalPrice = totalAmount.subtract(coupon.getDiscountAmount());
            } else if (coupon.getType() == 2) {
                actualTotalPrice = totalAmount.subtract(coupon.getDiscountAmount());
                if (actualTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
                    actualTotalPrice = BigDecimal.ZERO;
                }
            } else if (coupon.getType() == 3) {
                actualTotalPrice = totalAmount.multiply(coupon.getDiscountRate());
            }

            userCoupon.setStatus(1);
            userCoupon.setUseTime(java.time.LocalDateTime.now());
            userCoupon.setOrderId((long) orderIds.get(0));
            userCouponService.updateById(userCoupon);
        }

        User currentUser = TokenUtils.getCurrentUser();
        User user = userMapper.selectById(currentUser.getId());
        if (user.getAccount() < actualTotalPrice.doubleValue()) {
            throw new ServiceException("201", "余额不足，请充值~");
        }

        for (Integer orderId : orderIds) {
            Orders order = ordersMapper.selectById(orderId);
            order.setState("已支付");
            double proportion = order.getPrice() / totalAmount.doubleValue();
            double orderActualPrice = actualTotalPrice.doubleValue() * proportion;
            order.setActualPrice(orderActualPrice);
            order.setCouponId(couponId);
            ordersMapper.updateById(order);
        }

        user.setAccount(user.getAccount() - actualTotalPrice.doubleValue());
        userMapper.updateById(user);
    }
}
