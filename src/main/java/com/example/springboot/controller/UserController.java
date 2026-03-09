package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.service.IUserService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody User user) {
        try {
            userService.save(user);
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                return Result.error("插入数据库错误");
            } else {
                return Result.error("系统错误");
            }
        }
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (id.equals(currentUser.getId())) {
            throw new ServiceException("不能删除当前的用户");
        }
        userService.removeById(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        List<User> userList = userService.list(new QueryWrapper<User>().orderByDesc("id"));  // select * from user order by id desc
        return Result.success(userList);
    }

    /**
     * 获取详情
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return Result.success(user);
    }


    /**
     * 分页查询
     */
    @GetMapping("/selectByPage")
    public Result selectByPage(@RequestParam Integer pageNum,
                               @RequestParam Integer pageSize,
                               @RequestParam String username,
                               @RequestParam String role,
                               @RequestParam String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getId);  // 默认倒序，让最新的数据在最上面
        queryWrapper.like(StrUtil.isNotBlank(username), User::getUsername, username);
        queryWrapper.like(StrUtil.isNotBlank(name), User::getName, name);
        queryWrapper.eq(StrUtil.isNotBlank(role), User::getRole, role);
        // select * from user where username like '%#{username}%' and name like '%#{name}%'
        Page<User> page = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return Result.success(page);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result password(@RequestBody User user) {
        userService.updatePassword(user);
        return Result.success();
    }
}
