package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.example.springboot.common.AuthAccess;
import com.example.springboot.common.Result;
import com.example.springboot.entity.User;
import com.example.springboot.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 功能：提供接口返回数据
 */
@RestController
public class WebController {

    @Autowired
    private IUserService userService;

    @AuthAccess
    @GetMapping("/")
    public Result hello() {
        return Result.success("success");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            return Result.error("数据输入不合法");
        }
        user = userService.login(user);
        return Result.success(user);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            return Result.error("数据输入不合法");
        }
        if (user.getUsername().length() > 10 || user.getPassword().length() > 20) {
            return Result.error("数据输入不合法");
        }
        if (StrUtil.isBlank(user.getRole())) {
            user.setRole("USER");
        }
        user = userService.register(user);
        return Result.success(user);
    }

    /**
     *  重置密码
     */
    @PutMapping("/password")
    public Result password(@RequestBody User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPhone())) {
            return Result.error("数据输入不合法");
        }
        userService.resetPassword(user);
        return Result.success();
    }

}
