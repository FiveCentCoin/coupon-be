package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Coupon;
import com.example.springboot.entity.User;
import com.example.springboot.entity.UserCoupon;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.service.ICouponService;
import com.example.springboot.service.IUserCouponService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private ICouponService couponService;

    @Autowired
    private IUserCouponService userCouponService;

    @PostMapping("/create")
    public Result create(@RequestBody Coupon coupon) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        if (StrUtil.isBlank(coupon.getName())) {
            throw new ServiceException("优惠券名称不能为空");
        }

        if (coupon.getType() == null || coupon.getType() < 1 || coupon.getType() > 3) {
            throw new ServiceException("优惠券类型错误");
        }

        if (coupon.getType() == 1) {
            if (coupon.getMinAmount() == null || coupon.getDiscountAmount() == null) {
                throw new ServiceException("满减券必须设置最低消费金额和减免金额");
            }
        } else if (coupon.getType() == 2) {
            if (coupon.getDiscountAmount() == null) {
                throw new ServiceException("无门槛券必须设置减免金额");
            }
            coupon.setMinAmount(null);
        } else if (coupon.getType() == 3) {
            if (coupon.getDiscountRate() == null) {
                throw new ServiceException("折扣券必须设置折扣率");
            }
        }

        if (coupon.getStartTime() == null || coupon.getEndTime() == null) {
            throw new ServiceException("有效期不能为空");
        }

        if (coupon.getTotalCount() == null || coupon.getTotalCount() <= 0) {
            throw new ServiceException("发放总量必须大于0");
        }

        coupon.setUsedCount(0);
        coupon.setStatus(1);
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setUpdateTime(LocalDateTime.now());

        couponService.save(coupon);
        return Result.success();
    }

    @PostMapping("/distribute")
    public Result distribute(@RequestBody UserCoupon userCoupon) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        if (userCoupon.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }

        if (userCoupon.getCouponId() == null) {
            throw new ServiceException("优惠券ID不能为空");
        }

        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }

        if (coupon.getStatus() != 1) {
            throw new ServiceException("优惠券已禁用");
        }

        if (coupon.getUsedCount() >= coupon.getTotalCount()) {
            throw new ServiceException("优惠券已发放完");
        }

        if (LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new ServiceException("优惠券已过期");
        }

        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, userCoupon.getUserId());
        queryWrapper.eq(UserCoupon::getCouponId, userCoupon.getCouponId());
        long count = userCouponService.count(queryWrapper);
        if (count > 0) {
            throw new ServiceException("该用户已拥有此优惠券");
        }

        userCoupon.setStatus(0);
        userCoupon.setCreateTime(LocalDateTime.now());
        userCouponService.save(userCoupon);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        coupon.setUpdateTime(LocalDateTime.now());
        couponService.updateById(coupon);

        return Result.success();
    }

    @PostMapping("/distributeBatch")
    public Result distributeBatch(@RequestBody List<Long> userIds, @RequestParam Long couponId) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        if (userIds == null || userIds.isEmpty()) {
            throw new ServiceException("用户ID列表不能为空");
        }

        if (couponId == null) {
            throw new ServiceException("优惠券ID不能为空");
        }

        Coupon coupon = couponService.getById(couponId);
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }

        if (coupon.getStatus() != 1) {
            throw new ServiceException("优惠券已禁用");
        }

        if (LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new ServiceException("优惠券已过期");
        }

        int availableCount = coupon.getTotalCount() - coupon.getUsedCount();
        if (availableCount < userIds.size()) {
            throw new ServiceException("优惠券剩余数量不足");
        }

        int successCount = 0;
        for (Long userId : userIds) {
            LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserCoupon::getUserId, userId);
            queryWrapper.eq(UserCoupon::getCouponId, couponId);
            long count = userCouponService.count(queryWrapper);
            
            if (count == 0) {
                UserCoupon userCoupon = new UserCoupon();
                userCoupon.setUserId(userId);
                userCoupon.setCouponId(couponId);
                userCoupon.setStatus(0);
                userCoupon.setCreateTime(LocalDateTime.now());
                userCouponService.save(userCoupon);
                successCount++;
            }
        }

        coupon.setUsedCount(coupon.getUsedCount() + successCount);
        coupon.setUpdateTime(LocalDateTime.now());
        couponService.updateById(coupon);

        return Result.success("成功发放" + successCount + "张优惠券");
    }

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) Integer type) {
        LambdaQueryWrapper<Coupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(name), Coupon::getName, name);
        queryWrapper.eq(type != null, Coupon::getType, type);
        queryWrapper.orderByDesc(Coupon::getCreateTime);
        
        Page<Coupon> page = couponService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return Result.success(page);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Long id) {
        Coupon coupon = couponService.getById(id);
        return Result.success(coupon);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Coupon coupon) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        if (coupon.getId() == null) {
            throw new ServiceException("优惠券ID不能为空");
        }

        Coupon existCoupon = couponService.getById(coupon.getId());
        if (existCoupon == null) {
            throw new ServiceException("优惠券不存在");
        }

        coupon.setUpdateTime(LocalDateTime.now());
        couponService.updateById(coupon);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam Long id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        Coupon coupon = couponService.getById(id);
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }

        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getCouponId, id);
        long count = userCouponService.count(queryWrapper);
        if (count > 0) {
            throw new ServiceException("该优惠券已被用户领取，无法删除");
        }

        couponService.removeById(id);
        return Result.success();
    }

    @GetMapping("/userCoupon/myCoupons")
    public Result getMyCoupons() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, currentUser.getId());
        queryWrapper.orderByDesc(UserCoupon::getCreateTime);
        
        List<UserCoupon> userCoupons = userCouponService.list(queryWrapper);
        
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponService.getById(userCoupon.getCouponId());
            userCoupon.setCoupon(coupon);
            
            if (coupon != null && LocalDateTime.now().isAfter(coupon.getEndTime())) {
                userCoupon.setStatus(2);
                userCouponService.updateById(userCoupon);
            }
        }
        
        return Result.success(userCoupons);
    }
}
